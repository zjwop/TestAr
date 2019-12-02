package com.zjwop.ar.baggage

/**
 * Created by zhaojianwu on 2019-11-28.
 */
class BaggageResultInfo(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f){

    fun getXCm(): String{
        return format(x)
    }

    fun getYCm(): String{
        return format(y)
    }

    fun getZCm(): String{
        return format(z)
    }

    fun getLengthCm(): String{
        return if(x > z) format(x) else format(z)
    }

    fun getWidthCm(): String{
        return if(x > z) format(z) else format(x)
    }

    fun getHeightCm(): String{
        return format(y)
    }

    private fun format(floatNum: Float): String {
        return (floatNum * 100f).toInt().toString() + "cm"
    }
}