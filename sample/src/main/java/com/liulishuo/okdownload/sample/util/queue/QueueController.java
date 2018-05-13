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

package com.liulishuo.okdownload.sample.util.queue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.SeekBar;

import com.liulishuo.okdownload.DownloadContext;
import com.liulishuo.okdownload.DownloadContextListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.sample.R;
import com.liulishuo.okdownload.sample.util.DemoUtil;
import com.liulishuo.okdownload.sample.util.queue.QueueRecyclerAdapter.QueueViewHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueueController {
    private static final String TAG = "QueueController";
    private List<DownloadTask> taskList = new ArrayList<>();
    private DownloadContext context;
    private final QueueListener listener = new QueueListener();

    private File queueDir;

    public void initTasks(@NonNull Context context, @NonNull DownloadContextListener listener) {
        final DownloadContext.QueueSet set = new DownloadContext.QueueSet();
        final File parentFile = new File(DemoUtil.getParentFile(context), "queue");
        this.queueDir = parentFile;

        set.setParentPathFile(parentFile);
        set.setMinIntervalMillisCallbackProcess(200);

        final DownloadContext.Builder builder = set.commit();

        String url = "http://dldir1.qq.com/weixin/android/weixin6516android1120.apk";
        DownloadTask boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "1. WeChat");

        url = "https://cdn.llscdn.com/yy/files/tkzpx40x-lls-LLS-5.7-785-20171108-111118.apk";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "2. LiuLiShuo");

        url = "https://t.alipayobjects.com/L1/71/100/and/alipay_wap_main.apk";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "3. Alipay");

        url = "https://dldir1.qq.com/qqfile/QQforMac/QQ_V6.2.0.dmg";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "4. QQ for Mac");

        final String zhiHuApkHome = "https://zhstatic.zhihu.com/pkg/store/zhihu";
        url = zhiHuApkHome + "/futureve-mobile-zhihu-release-5.8.2(596).apk";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "5. ZhiHu");

        url = "http://d1.music.126.net/dmusic/CloudMusic_official_4.3.2.468990.apk";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "6. NetEaseMusic");

        url = "http://d1.music.126.net/dmusic/NeteaseMusic_1.5.9_622_officialsite.dmg";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "7. NetEaseMusic for Mac");

        url = "http://dldir1.qq.com/weixin/Windows/WeChatSetup.exe";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "8. WeChat for Windows");

        url = "https://dldir1.qq.com/foxmail/work_weixin/wxwork_android_2.4.5.5571_100001.apk";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "9. WeChat Work");

        url = "https://dldir1.qq.com/foxmail/work_weixin/WXWork_2.4.5.213.dmg";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "10. WeChat Work for Mac");

        builder.setListener(listener);

        this.context = builder.build();
        this.taskList = Arrays.asList(this.context.getTasks());
    }

    public void deleteFiles() {
        if (queueDir != null) {
            String[] children = queueDir.list();
            if (children != null) {
                for (String child : children) {
                    if (!new File(queueDir, child).delete()) {
                        Log.w("QueueController", "delete " + child + " failed!");
                    }
                }
            }

            if (!queueDir.delete()) {
                Log.w("QueueController", "delete " + queueDir + " failed!");
            }
        }

        for (DownloadTask task : taskList) {
            TagUtil.clearProceedTask(task);
        }
    }

    public void setPriority(DownloadTask task, int priority) {
        final DownloadTask newTask = task.toBuilder().setPriority(priority).build();
        this.context = context.toBuilder()
                .bindSetTask(newTask)
                .build();
        newTask.setTags(task);
        TagUtil.savePriority(newTask, priority);
        this.taskList = Arrays.asList(this.context.getTasks());
    }

    public void start(boolean isSerial) {
        this.context.start(listener, isSerial);
    }

    public void stop() {
        if (this.context.isStarted()) {
            this.context.stop();
        }
    }

    void bind(final QueueViewHolder holder, int position) {
        final DownloadTask task = taskList.get(position);
        Log.d(TAG, "bind " + position + " for " + task.getUrl());

        listener.bind(task, holder);
        listener.resetInfo(task, holder);

        // priority
        final int priority = TagUtil.getPriority(task);
        holder.priorityTv
                .setText(holder.priorityTv.getContext().getString(R.string.priority, priority));
        holder.prioritySb.setProgress(priority);
        if (this.context.isStarted()) {
            holder.prioritySb.setEnabled(false);
        } else {
            holder.prioritySb.setEnabled(true);
            holder.prioritySb.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        boolean isFromUser;

                        @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                                boolean fromUser) {
                            isFromUser = fromUser;
                        }

                        @Override public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override public void onStopTrackingTouch(SeekBar seekBar) {
                            if (isFromUser) {
                                final int priority = seekBar.getProgress();
                                setPriority(task, priority);
                                holder.priorityTv
                                        .setText(seekBar.getContext()
                                                .getString(R.string.priority, priority));
                            }
                        }
                    });
        }
    }

    int size() {
        return taskList.size();
    }
}