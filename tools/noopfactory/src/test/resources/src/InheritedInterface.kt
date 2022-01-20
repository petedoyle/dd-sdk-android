package com.example

import co.fast.android.internal.datadog.tools.annotation.NoOpImplementation

interface RootInterface {

    fun rootMethod()
}

interface ParentInterface : RootInterface {

    fun parentMethod()
}

@NoOpImplementation
interface InheritedInterface : ParentInterface {

    fun doSomething()
}
