# Basics

This library provides some concise methods base on kotlin to make it easier to use OkDownload. 
Combining kotlin coroutine in the meantime.

## Download Listener

There are easier ways to create kinds of listeners.

### DownloadListener

The most concise way:
```kotlin
val listener = createListener { task, cause, realCause ->
}
```
Only `taskEnd` callback is necessary. If you concern `downloadFromBeginning` callback, 
just add a parameter:
```kotlin
val listener = createListener(
    onDownloadFromBeginning = {task, info, cause -> }
) { task, cause, realCause ->
}
```
### DownloadListener3

The most concise way:
```kotlin
val listener3 = createListener3 {}
```
Only a terminal block is necessary and it's type is `() -> Unit`. This terminal action will be 
invoked as long as `warn` or `error` or `completed` or `canceled` occurs.

If you concern progress callback, just add a parameter:
```kotlin
val listener3 = createListener3(
    onProgress = { task, currentOffset, totalLength -> }
) {}
```
### Others

The rest listeners contain:

- DownloadListener2
- DownloadListener4
- DownloadListener4WithSpeed
- DownloadContextListener

The concise way to create them is the same as creating `DownloadListener`. 

## Start Task 

### Base on kotlin extension function

There are concise methods to start a task, consistent with creating listeners.

| start task | create listener |
| ---------- | --------------- |
| enqueue/execute | createListener  |
| enqueue1/execute1 | createListener1 |
| enqueue2/execute2 | createListener2 |
| enqueue3/execute3 | createListener3 |
| enqueue4/execute4 | createListener4 |
| enqueue4WithSpeed/execute4WithSpeed | createListener4WithSpeed |

These methods are kotlin extension methods of DownloadTask, no need to concern the creation of 
listener. For example:

```kotlin
lateinit var task: DownloadTask
// initial task
// start task
task.enqueue { task, cause, realCause ->
}
```

### Base on kotlin coroutine

```kotlin
lateinit var task: DownloadTask
// initial task
// start task
lifecycleScope.launch {
    val downloadResult = task.await()
}
```

## Show Progress

There are new two ways to show download progress.
 
### Base on listener

```kotlin
lateinit var task: DownloadTask
// initial task
// start task
task.enqueue3(
    onProgress = { task, currentOffset, totalLength -> 
        // show progress
    }
) { task, cause, realCause ->
}
```

### Base on kotlin channel

```kotlin
lateinit var task: DownloadTask
// initial task
// start task
task.enqueue3 { task, cause, realCause ->
}
// must after starting task
val pb = task.spChannel()
lifecycleScope.launch {
    for (progress in pb) {
        // show progress
    }
}
```