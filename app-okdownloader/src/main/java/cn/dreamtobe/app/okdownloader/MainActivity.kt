package cn.dreamtobe.app.okdownloader

import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        initSingleTaskDemo()
    }

    private fun initSingleTaskDemo() {
        val demo = SingleTaskDemo()
        val storePath = Uri.fromFile(externalCacheDir)

        startSameTaskBtn.isEnabled = false
        startSameFileBtn.isEnabled = false

        startOrCancelBtn.setOnClickListener { v ->
            run {
                if (v.tag == null) {
                    demo.startAsync(this)
                    (v as AppCompatButton).setText(R.string.cancel_download)
                    v.tag = Object()
                    startSameTaskBtn.isEnabled = true
                    startSameFileBtn.isEnabled = true
                } else {
                    demo.cancelTask()
                    v.tag = null
                    (v as AppCompatButton).setText(R.string.start_download)
                    startSameTaskBtn.isEnabled = false
                    startSameFileBtn.isEnabled = false
                }
            }
        }
        startSameTaskBtn.setOnClickListener { demo.startSameTask_sameTaskBusy(storePath) }
        startSameFileBtn.setOnClickListener { demo.startSamePathTask_fileBusy(storePath) }
    }
}
