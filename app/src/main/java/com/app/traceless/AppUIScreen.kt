package com.app.traceless

import com.app.traceless.analytic.UIScreen

sealed class AppUIScreen{
    data object Feature : UIScreen("feature")

    data object Detail: UIScreen("detail")
}
