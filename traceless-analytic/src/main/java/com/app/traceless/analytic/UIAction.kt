// src/main/kotlin/com/app/traceless/analytic/UIAction.kt
package com.app.traceless.analytic

open class UIAction( val value: String){

    // Basic UI actions provided by the SDK
    object Click : UIAction("click")
    object Select : UIAction("select")
    object Input : UIAction("input")
    object Swipe : UIAction("swipe")
    object Submit : UIAction("submit")
    object Toggle : UIAction("toggle")
    object Navigate : UIAction("navigate")
    object View : UIAction("view")
    object Refresh : UIAction("refresh")
    object Dismiss : UIAction("dismiss")
}



// Complex actions should be defined by the consuming app
