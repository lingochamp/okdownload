package com.liulishuo.okdownload.sample.base

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.liulishuo.okdownload.sample.R

abstract class BaseSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(titleRes())
    }

    @StringRes abstract fun titleRes(): Int

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_github -> {
                openGithub()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openGithub() {
        val uri = Uri.parse(getString(R.string.github_url))
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}