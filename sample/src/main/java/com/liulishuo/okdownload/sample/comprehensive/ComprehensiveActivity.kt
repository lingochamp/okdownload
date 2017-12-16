/*
 * Copyright (c) 2017 LingoChamp Inc.
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

package com.liulishuo.okdownload.sample.comprehensive

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import com.liulishuo.okdownload.UnifiedListenerManager
import com.liulishuo.okdownload.sample.R
import com.liulishuo.okdownload.sample.base.BaseSampleActivity
import com.liulishuo.okdownload.sample.comprehensive.multiple.MultipleTaskFragment
import com.liulishuo.okdownload.sample.comprehensive.queue.QueueFragment
import com.liulishuo.okdownload.sample.comprehensive.single.SingleTaskFragment
import kotlinx.android.synthetic.main.activity_main.*

/**
 * On this demo you can see a comprehensive demo with several tabs switch among pages.
 */
class ComprehensiveActivity : BaseSampleActivity() {

    override fun titleRes() = R.string.comprehensive_case_title

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_single -> {
                fragmentManager.beginTransaction().replace(
                        R.id.container, singleTaskFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_multiple -> {
                fragmentManager.beginTransaction().replace(
                        R.id.container, multipleTaskFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_queue -> {
                fragmentManager.beginTransaction().replace(
                        R.id.container, queueFragment).commit()
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
}
