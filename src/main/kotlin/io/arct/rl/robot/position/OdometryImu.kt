package io.arct.rl.robot.position

import io.arct.rl.hardware.sensors.DistanceEncoder
import io.arct.rl.units.Angle
import io.arct.rl.units.Coordinates
import io.arct.rl.units.Distance
import io.arct.rl.units.rad
import kotlin.math.PI

class OdometryImu(
    private val y1: DistanceEncoder,
    private val y2: DistanceEncoder,
    private val x: DistanceEncoder,
    private val diameter: Distance
) : DynamicPositioning() {

    override var rotation: Angle
        get() = super.rotation
        set(value) {}

    private var y1d: Distance = y1.position
    private var y1a: Angle = y1.angle
    private var y2d: Distance = y2.position
    private var y2a: Angle = y2.angle
    private var x1d: Distance = x.position

    override fun zero(): OdometryImu {
        y1.zero()
        y2.zero()
        x.zero()
        push()
        return this
    }

    override fun updateLinear() {
        val dy1 = y1.position - y1d
        val dy2 = y2.position - y2d
        val dx1 = x.position - x1d

        val dy = (dy1 + dy2) / 2
        val dx = dx1

        position = Coordinates(position.x + dx, position.y + dy)
        push()
    }

    override fun updateAngular() {
        TODO()
//        rotation += dt
//        push()
    }

    private fun push() {
        y1d = y1.position
        y1a = y1.angle
        y2d = y2.position
        y2a = y2.angle
        x1d = x.position
    }

}