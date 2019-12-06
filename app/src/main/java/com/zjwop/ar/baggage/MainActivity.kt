package com.zjwop.ar.baggage

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.zjwop.ar.R

/**
 * Created by zhaojianwu on 2019-12-03.
 */
class MainActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.auto_mode).setOnClickListener {
            Intent(this, ArMeasureBaggageAutoActivity::class.java).let {
                startActivity(it)
            }
        }

        findViewById<Button>(R.id.auto_mode_v2).setOnClickListener {
            Intent(this, ArMeasureBaggageAutoActivityV2::class.java).let {
                startActivity(it)
            }
        }

        findViewById<Button>(R.id.manual_mode).setOnClickListener {
            Intent(this, ArMeasureBaggageActivity::class.java).let {
                startActivity(it)
            }
        }

        findViewById<Button>(R.id.manual_mode_v2).setOnClickListener {
            Intent(this, ArMeasureBaggageActivityV2::class.java).let {
                startActivity(it)
            }
        }
    }
}