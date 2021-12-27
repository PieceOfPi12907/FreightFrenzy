package org.firstinspires.ftc.teamcode.helper;


import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import static java.lang.Thread.sleep;

public class NavigationHelper {

    Orientation lastAngles = new Orientation();
    double globalAngle;
    //LinearOpMode opm = new LinearOpMode() {

    // This method tells us - Based on the direction we want to move(STRAIGHT,LEFT,RIGHT,TURN), it will call the needed method with parameters
    public void navigate (double pTgtDistance, Constants2022.Direction pDirection, double pRotation, double pSpeed, DcMotor pBackLeft, DcMotor pBackRight, DcMotor pFrontRight, DcMotor pFrontLeft, BNO055IMU pImu, Telemetry telemetry, float header, boolean isForward ){

        if(pDirection.equals(Constants2022.Direction.STRAIGHT)){
            forwardDrive(pTgtDistance, pSpeed, pBackLeft, pBackRight, pFrontRight, pFrontLeft, telemetry, pImu, isForward );
        }

        else if(pDirection.equals(Constants2022.Direction.LEFT)){
            leftStrafe(pTgtDistance, pSpeed, pBackLeft, pBackRight, pFrontRight, pFrontLeft, pImu,telemetry);

        }
        else if(pDirection.equals(Constants2022.Direction.RIGHT)){
            rightStrafe(pTgtDistance, pSpeed, pBackLeft, pBackRight, pFrontRight, pFrontLeft, pImu, telemetry);
        }


        else if(pDirection.equals(Constants2022.Direction.TURN)){
            turnTest( pFrontLeft,  pFrontRight,  pBackLeft,  pBackRight, pImu, telemetry,  pRotation, pSpeed);
        }

    }




    // This is the method that gets called if constant is STRAIGHT
    public void forwardDrive (double pTgtDistance, double pSpeed, DcMotor pBackLeft, DcMotor pBackRight, DcMotor pFrontRight, DcMotor pFrontLeft, Telemetry telemetry, BNO055IMU pImu, boolean isForward) {
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json";
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.mode = BNO055IMU.SensorMode.IMU;
        pImu.initialize(parameters);

        pFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        pBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);


        ElapsedTime runtime = new ElapsedTime();

        PIDController pidDrive = new PIDController(0.05, 0.01, 0.01);
        // 0.05, 0.01, 0.01

        //Variables used for converting inches to Encoder dounts
        final double COUNTS_PER_MOTOR_DCMOTOR = 1120;    // eg: TETRIX Motor Encoder
        final double DRIVE_GEAR_REDUCTION = 0.5;     // This is < 1.0 if geared UP
        final double WHEEL_DIAMETER_INCHES = 3.93701;     // For figuring circumference
        final double COUNTS_PER_INCH = (COUNTS_PER_MOTOR_DCMOTOR * DRIVE_GEAR_REDUCTION) /
                (WHEEL_DIAMETER_INCHES * 3.1415);

        // newTargetPosition is the target position after it has been converted
        int newTargetPositionRight;
        int newTargetPositionLeft;
        // Sets all encoder values to 0 as we are moving robot with all 4 encoders
        pBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Determine new target position, converts it, and pass to motor controller
        newTargetPositionLeft = pBackLeft.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionRight = pBackRight.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionLeft = pFrontLeft.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionRight = pFrontRight.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        pBackLeft.setTargetPosition(newTargetPositionLeft);
        pBackRight.setTargetPosition(newTargetPositionRight);
        pFrontLeft.setTargetPosition(newTargetPositionLeft);
        pFrontRight.setTargetPosition(newTargetPositionRight);
        runtime.reset();

        telemetry.addData("Initial Value", "Running at %7d :%7d",
                pBackLeft.getCurrentPosition(), pBackRight.getCurrentPosition());
        telemetry.addData("New Target Position","Left %7d : Right %7d", newTargetPositionLeft,newTargetPositionRight);
        telemetry.addData("Initial Value", "Running at %7d :%7d",
                pFrontLeft.getCurrentPosition(), pFrontRight.getCurrentPosition());
        telemetry.update();

        pBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pFrontLeft.setMode((DcMotor.RunMode.RUN_TO_POSITION));
        pFrontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        /*pFrontRight.setPower((pSpeed));
        pBackRight.setPower((pSpeed));
        pFrontLeft.setPower((pSpeed));
        pBackLeft.setPower((pSpeed));*/

        pidDrive.setSetpoint(0);
        pidDrive.setOutputRange(0, pSpeed);
        pidDrive.setInputRange(90, 90);
        pidDrive.enable();
        double correction;

        telemetry.addLine("entering loop");
        telemetry.update();

        try {
            Thread.sleep(250);
        } catch(InterruptedException E){

        }

        lastAngles = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        //This while loop will keep the motors running to the target position until one of the motors have reached the final encoder count
        while ((pBackLeft.isBusy() && pBackRight.isBusy() && pFrontLeft.isBusy() && pFrontRight.isBusy()) ) {
            //set to 0 NO PID
            correction = pidDrive.performPID(getAngle(pImu))/2;
            pFrontRight.setPower(pSpeed + correction);
            pBackRight.setPower(pSpeed + correction);
            pFrontLeft.setPower(pSpeed - correction);
            pBackLeft.setPower(pSpeed - correction);

            telemetry.addData("imu first angle: ", pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle);
            telemetry.addData("imu last angle: ", lastAngles.firstAngle);
            telemetry.addData("imu header angle: ", getAngle(pImu));
            telemetry.update();

            try {
                Thread.sleep(100);
            } catch(InterruptedException E){

            }
        }

        //stop motors
        pFrontLeft.setPower(0);
        pFrontRight.setPower(0);
        pBackLeft.setPower(0);
        pBackRight.setPower(0);

        telemetry.addData("Final Position", "Running at %7d :%7d : %7d: %7d",
                pBackLeft.getCurrentPosition(),
                pBackRight.getCurrentPosition(),
                pFrontLeft.getCurrentPosition(),
                pFrontRight.getCurrentPosition());
        telemetry.update();
    }

    private double getAngle(BNO055IMU pImu)
    {
        // We experimentally determined the Z axis is the axis we want to use for heading angle.
        // We have to process the angle because the imu works in euler angles so the Z axis is
        // returned as 0 to +180 or 0 to -180 rolling back to -179 or +179 when rotation passes
        // 180 degrees. We detect this transition and track the total cumulative angle of rotation.

        Orientation angles = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);


        double deltaAngle = angles.firstAngle - lastAngles.firstAngle;

        if (deltaAngle < -180)
            deltaAngle += 360;
        else if (deltaAngle > 180)
            deltaAngle -= 360;


        //globalAngle += deltaAngle
        //globalAngle += deltaAngle;

//        lastAngles = angles;

        return deltaAngle;
    }


    private void leftStrafe(double pTgtDistance, double pSpeed, DcMotor pBackLeft, DcMotor pBackRight, DcMotor pFrontRight, DcMotor pFrontLeft,  BNO055IMU pImu, Telemetry telemetry) {
        ElapsedTime runtime = new ElapsedTime();

        final double COUNTS_PER_MOTOR_DCMOTOR = 1120;    // eg: TETRIX Motor Encoder
        final double DRIVE_GEAR_REDUCTION = 0.5;     // This is < 1.0 if geared UP
        final double WHEEL_DIAMETER_INCHES = 3.93701;     // For figuring circumference
        final double COUNTS_PER_INCH = (COUNTS_PER_MOTOR_DCMOTOR * DRIVE_GEAR_REDUCTION) /
                (WHEEL_DIAMETER_INCHES * 3.1415);
        int newTargetPositionFrontRight;
        int newTargetPositionFrontLeft;
        int newTargetPositionBackRight;
        int newTargetPositionBackLeft;

        double startingAngle = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                AngleUnit.DEGREES).firstAngle;

        pBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Determine new target position, and pass to motor controller
        newTargetPositionBackLeft = pBackLeft.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionBackRight = pBackRight.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionFrontLeft = pFrontLeft.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionFrontRight = pFrontRight.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        pBackLeft.setTargetPosition(newTargetPositionBackLeft);
        pBackRight.setTargetPosition(-(newTargetPositionBackRight));
        pFrontRight.setTargetPosition(newTargetPositionFrontRight);
        pFrontLeft.setTargetPosition(-(newTargetPositionBackLeft));
        runtime.reset();

        telemetry.addData("Initial Value", "Running at %7d :%7d",
                pBackLeft.getCurrentPosition(), pBackRight.getCurrentPosition(), pFrontLeft.getCurrentPosition(), pFrontRight.getCurrentPosition());
        telemetry.update();

        pBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pFrontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pFrontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        pFrontRight.setPower((pSpeed));
        pBackRight.setPower(-(pSpeed));
        pFrontLeft.setPower(-(pSpeed));
        pBackLeft.setPower((pSpeed));

        telemetry.addData("Path1", "Target Position %7d :%7d", newTargetPositionBackLeft, newTargetPositionBackRight, newTargetPositionFrontLeft, newTargetPositionFrontRight);

        telemetry.update();

        while ((pBackLeft.isBusy() && pBackRight.isBusy() && pFrontLeft.isBusy() && pFrontRight.isBusy())) {
            // Display it for the driver.
            //potentially put an "if stop pressed: exit while loop because 'stop' is depricated
        }

        //stop motors
        pFrontLeft.setPower(0);
        pFrontRight.setPower(0);
        pBackLeft.setPower(0);
        pBackRight.setPower(0);

        telemetry.addData("Final Position", "Running at %7d :%7d",
                pBackLeft.getCurrentPosition(),
                pBackRight.getCurrentPosition(),
                pFrontLeft.getCurrentPosition(),
                pFrontRight.getCurrentPosition());
        telemetry.update();

        //this.strafeCorrection(pBackLeft, pBackRight, pFrontRight, pFrontLeft, pImu, telemetry, startingAngle);
    }

    private void strafeCorrection (DcMotor pBackLeft, DcMotor pBackRight, DcMotor pFrontRight, DcMotor pFrontLeft,  BNO055IMU pImu, Telemetry pTelemetry, double pStartAngle) {

        double currentAngle = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                AngleUnit.DEGREES).firstAngle;

        double correction = pStartAngle-currentAngle;
        if(Math.abs(correction)>5){
            pTelemetry.addData("Correction value: ",correction);
            pTelemetry.update();
            turnWithEncoders(pFrontRight,pFrontLeft,pBackRight,pBackLeft, correction,0.15,pImu,pTelemetry);

        }

    }

    private void rightStrafe(double pTgtDistance, double pSpeed, DcMotor pBackLeft, DcMotor pBackRight, DcMotor pFrontRight, DcMotor pFrontLeft,  BNO055IMU pImu, Telemetry telemetry) {
        ElapsedTime runtime = new ElapsedTime();

        final double COUNTS_PER_MOTOR_DCMOTOR = 1120;    // eg: TETRIX Motor Encoder
        final double DRIVE_GEAR_REDUCTION = 0.5;     // This is < 1.0 if geared UP
        final double WHEEL_DIAMETER_INCHES = 3.93701;     // For figuring circumference
        final double COUNTS_PER_INCH = (COUNTS_PER_MOTOR_DCMOTOR * DRIVE_GEAR_REDUCTION) /
                (WHEEL_DIAMETER_INCHES * 3.1415);
        int newTargetPositionFrontRight;
        int newTargetPositionFrontLeft;
        int newTargetPositionBackRight;
        int newTargetPositionBackLeft;

        double startingAngle = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                AngleUnit.DEGREES).firstAngle;

        pBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Determine new target position, and pass to motor controller
        newTargetPositionBackLeft = pBackLeft.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionBackRight = pBackRight.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionFrontLeft = pFrontLeft.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        newTargetPositionFrontRight = pFrontRight.getCurrentPosition() + (int) (pTgtDistance * COUNTS_PER_INCH);
        pBackLeft.setTargetPosition(-(newTargetPositionBackLeft));
        pBackRight.setTargetPosition(newTargetPositionBackRight);
        pFrontRight.setTargetPosition(-(newTargetPositionFrontRight));
        pFrontLeft.setTargetPosition(newTargetPositionBackLeft);
        runtime.reset();

        telemetry.addData("Initial Value", "Running at %7d :%7d",
                pBackLeft.getCurrentPosition(), pBackRight.getCurrentPosition(), pFrontLeft.getCurrentPosition(), pFrontRight.getCurrentPosition());
        telemetry.update();

        pBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pFrontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        pFrontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        pFrontRight.setPower(-(pSpeed));
        pBackRight.setPower((pSpeed));
        pFrontLeft.setPower((pSpeed));
        pBackLeft.setPower(-(pSpeed));

        telemetry.addData("Path1", "Target Position %7d :%7d", newTargetPositionBackLeft, newTargetPositionBackRight, newTargetPositionFrontLeft, newTargetPositionFrontRight);
        telemetry.update();

        while ((pBackLeft.isBusy() && pBackRight.isBusy() && pFrontLeft.isBusy() && pFrontRight.isBusy())) {

            // Display it for the driver.
            /*telemetry.addData("Path1", "Running to %7d :%7d", newTargetPositionBackLeft, newTargetPositionBackRight, newTargetPositionFrontLeft, newTargetPositionFrontRight);
            telemetry.addData("Path2", "Running at %7d :%7d",
                    pBackLeft.getCurrentPosition(),
                    pBackRight.getCurrentPosition(),
                    pFrontLeft.getCurrentPosition(),
                    pFrontRight.getCurrentPosition());
            telemetry.update();*/
        }

        //stop motors
        pFrontLeft.setPower(0);
        pFrontRight.setPower(0);
        pBackLeft.setPower(0);
        pBackRight.setPower(0);

        telemetry.addData("Final Position", "Running at %7d :%7d",
                pBackLeft.getCurrentPosition(),
                pBackRight.getCurrentPosition(),
                pFrontLeft.getCurrentPosition(),
                pFrontRight.getCurrentPosition());
        telemetry.update();

        this.strafeCorrection(pBackLeft, pBackRight, pFrontRight, pFrontLeft, pImu, telemetry, startingAngle);


    }

    public void turnWithEncoders(DcMotor pFrontRight, DcMotor pFrontLeft, DcMotor pBackRight,
                                 DcMotor pBackLeft, double pRotation, double pSpeed, BNO055IMU pImu, Telemetry pTelemetry) {
        double computedAngle = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                AngleUnit.DEGREES).firstAngle;
        double currentAngle = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                AngleUnit.DEGREES).firstAngle;
        double previousAngle = 0.0;
        pTelemetry.addData("Initial Angle: ", computedAngle);
        pTelemetry.update();



        pFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        pFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        pBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        pBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        if (pRotation > 0) {
            while (computedAngle < pRotation) {
                //right
                previousAngle = computedAngle;
                pFrontRight.setPower(pSpeed);
                pBackRight.setPower(pSpeed);
                pFrontLeft.setPower(-pSpeed);
                pBackLeft.setPower(-pSpeed);
                computedAngle = ((pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                        AngleUnit.DEGREES)).firstAngle) - currentAngle;
            }
        } else {
            while (computedAngle > pRotation) {
                //left
                previousAngle = computedAngle;
                pFrontRight.setPower(-pSpeed);
                pBackRight.setPower(-pSpeed);
                pFrontLeft.setPower(pSpeed);
                pBackLeft.setPower(pSpeed);
                computedAngle = ((pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                        AngleUnit.DEGREES)).firstAngle) - currentAngle;
            }

        }
        pFrontRight.setPower(0);
        pBackRight.setPower(0);
        pFrontLeft.setPower(0);
        pBackLeft.setPower(0);
        currentAngle = pImu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                AngleUnit.DEGREES).firstAngle;
        pTelemetry.addData("Previous Angle: ",previousAngle);
        pTelemetry.addData("Computed Angle: ",computedAngle);

        pTelemetry.addData("Current Angle: ",currentAngle);
        pTelemetry.update();
    }


    public void turnTest(DcMotor frontLeft, DcMotor frontRight, DcMotor backLeft, DcMotor backRight, BNO055IMU imu, Telemetry telemetry, double turn, double speed){

        frontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);


        double error = speed*10*3 - 7;
        double currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                AngleUnit.DEGREES).firstAngle;

        telemetry.addData("curr:", currentAngle);
        telemetry.update();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (turn < 0) {

            double math = currentAngle + turn;

            if (math<-180) {
                math = 180 - Math.abs(-180-math);
            }
            telemetry.addData("goalAngle:", math);
            telemetry.update();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (Math.abs(currentAngle - math) > error) {
                frontLeft.setPower(speed);
                backLeft.setPower(speed);
                frontRight.setPower(-speed);
                backRight.setPower(-speed);

                currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                        AngleUnit.DEGREES).firstAngle;

            }

            frontLeft.setPower(0);
            backLeft.setPower(0);
            frontRight.setPower(0);
            backRight.setPower(0);



        }
        else{
            double math = currentAngle + turn;
            if (math>180) {
                math = -180 + Math.abs(180-math);
            }
            telemetry.addData("goalAngle:", math);
            telemetry.update();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (Math.abs(currentAngle - math) > error) {
                frontLeft.setPower(-speed);
                backLeft.setPower(-speed);
                frontRight.setPower(speed);
                backRight.setPower(speed);

                currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                        AngleUnit.DEGREES).firstAngle;

            }
            frontLeft.setPower(0);
            backLeft.setPower(0);
            frontRight.setPower(0);
            backLeft.setPower(0);

        }
    }

}