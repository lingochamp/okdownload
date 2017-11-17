/*
 * Copyright (C) 2017 Jacksgong(jacksgong.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.okdownload.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.liulishuo.okdownload.UnifiedListenerManager
import com.liulishuo.okdownload.sample.multiple.MultipleTaskFragment
import com.liulishuo.okdownload.sample.queue.QueueFragment
import com.liulishuo.okdownload.sample.single.SingleTaskFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_single -> {
                fragmentManager.beginTransaction().replace(R.id.container, singleTaskFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_multiple -> {
                fragmentManager.beginTransaction().replace(R.id.container, multipleTaskFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_queue -> {
                fragmentManager.beginTransaction().replace(R.id.container, queueFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private lateinit var singleTaskFragment: SingleTaskFragment
    private lateinit var multipleTaskFragment: MultipleTaskFragment
    private lateinit var queueFragment: QueueFragment

    // Because I want same task with several listener, so use UnifiedListenerManager
    val listenerManager = UnifiedListenerManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        singleTaskFragment = SingleTaskFragment.newInstance()
        multipleTaskFragment = MultipleTaskFragment.newInstance()
        queueFragment = QueueFragment.newInstance()

        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        fragmentManager.beginTransaction().replace(R.id.container, singleTaskFragment).commit()
    }

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
