// src/main/kotlin/com/app/traceless/analytic/UIScreen.kt
package com.app.traceless.analytic

open class UIScreen(val name: String){

    data object Splash : UIScreen("splash")
    data object Main : UIScreen("main")

}
// Additional screens can be added as needed here if required by the app
