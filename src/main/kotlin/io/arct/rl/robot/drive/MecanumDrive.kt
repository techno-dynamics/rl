package io.arct.rl.robot.drive

import io.arct.rl.eventloop.Program
import io.arct.rl.extensions.normalize
import io.arct.rl.hardware.motors.Motor
import io.arct.rl.navigation.DirectedPath
import io.arct.rl.robot.position.DynamicPositioning
import io.arct.rl.robot.position.TripleOdometry
import io.arct.rl.units.*
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos

class MecanumDrive(
    vararg val motors: Motor,
    var invert: Boolean = false,
    val program: Program? = null
) : Drive() {
    override val velocity: Velocity = motors[0].velocity

    override fun move(direction: Angle, speed: Velocity): MecanumDrive {
        val dir = if (invert) -direction.deg else direction.deg

        val a = sin((dir + 45.deg).rad.value)
        val b = cos((dir + 45.deg).rad.value)

        val (x, y) = Double.normalize(a, b, upscale = true).map { speed * it }

        motors[0].power(x)
        motors[1].power(y)
        motors[2].power(x)
        motors[3].power(y)

        DynamicPositioning.updateLinear(robot.positioning)
        return this
    }

    override fun turn(speed: Velocity, rotationSpeed: Velocity): MecanumDrive {
        val velocity = motors[0].velocity

        val a = speed + if (rotationSpeed > 0.cmps) rotationSpeed else 0.0.cmps
        val b = speed - if (rotationSpeed < 0.cmps) rotationSpeed else 0.0.cmps

        val (l, r) = Double.normalize(a / velocity, b / velocity)

        motors[0].power(l)
        motors[1].power(l)
        motors[2].power(r)
        motors[3].power(r)

        DynamicPositioning.updateAngular(robot.positioning)
        return this
    }

    override suspend fun move(direction: Angle, distance: Distance, speed: Velocity): MecanumDrive {
        val initial = robot.position
        move(direction, speed)

        while (initial distance robot.position <= distance && (program == null || program.active)) {
            DynamicPositioning.updateLinear(robot.positioning)

//            val odometry: TripleOdometry = robot.positioning as? TripleOdometry
//                ?: continue
//
//            val y1 = odometry.y1.position
//            val y2 = odometry.y2.position
//
//            if (direction.deg.general == Angle.Forward.deg.general) when {
//                abs((y2 - y1).cm.value) < 3 ->                 move(direction, speed)
//                y1 > y2 -> turn(speed - velocity * 0.1, speed)
//                y1 < y2 -> turn(speed - velocity, -speed)
//            }
//
//            if (direction.deg.general == Angle.Backward.deg.general) when {
//                abs((y2 - y1).cm.value) < 3 ->                 move(direction, speed)
//                y1 < y2 -> turn(speed - velocity * 0.1, speed)
//                y1 > y2 -> turn(speed - velocity, -speed)
//            }
        }

        stop()

        return this
    }

    override suspend fun move(path: DirectedPath): MecanumDrive {
        val initial = robot.position

        do {
            val pos = initial distance robot.position
            val speed = path.path(pos)

            move(path.direction, speed)
            delay(10)

            DynamicPositioning.updateLinear(robot.positioning)
        } while (speed.value != 0.0 && (program == null || program.active))

        stop()
        return this
    }

    override suspend fun rotate(angle: Angle, speed: Velocity): MecanumDrive {
        val initial = robot.rotation

        rotate(speed)

        while (abs((robot.rotation - initial).deg.value) <= angle.deg.value && (program == null || program.active))
            DynamicPositioning.updateAngular(robot.positioning)

        stop()
        return this
    }

    override fun rotate(speed: Velocity): MecanumDrive {
        motors[0].power(speed)
        motors[1].power(speed)
        motors[2].power(-speed)
        motors[3].power(-speed)

        DynamicPositioning.updateAngular(robot.positioning)
        return this
    }
}