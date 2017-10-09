package cn.dreamtobe.okdownload.benchmark

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import cn.dreamtobe.app.okdownloader.R
import cn.dreamtobe.okdownload.DownloadListener
import cn.dreamtobe.okdownload.OkDownload
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointInfo
import cn.dreamtobe.okdownload.core.connection.DownloadConnection
import cn.dreamtobe.okdownload.DownloadTask
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                message.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                message.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                message.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        OkDownload.obtainTask("", application.cacheDir)
                .enqueue(object : DownloadListener {
                    override fun connectStart(task: DownloadTask?, blockIndex: Int) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun connectEnd(task: DownloadTask?, blockIndex: Int, connection: DownloadConnection?, connected: DownloadConnection.Connected?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun taskStart(task: DownloadTask?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun breakpointData(task: DownloadTask?, info: BreakpointInfo?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun downloadFromBeginning(task: DownloadTask?, info: BreakpointInfo?, cause: DownloadListener.ResumeFailedCause?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun downloadFromBreakpoint(task: DownloadTask?, info: BreakpointInfo?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun fetchStart(task: DownloadTask?, blockIndex: Int, contentLength: Long) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun fetchProgress(task: DownloadTask?, blockIndex: Int, fetchedBytes: Long) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun fetchEnd(task: DownloadTask?, blockIndex: Int, contentLength: Long) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun taskEnd(task: DownloadTask?, cause: DownloadListener.EndCause?, realCause: Exception?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                })
    }
}
