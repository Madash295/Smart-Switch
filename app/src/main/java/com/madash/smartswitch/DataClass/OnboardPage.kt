package com.madash.smartswitch.com.madash.smartswitch.DataClass

import com.madash.smartswitch.R

data class OnboardPage(
    val title : String,
    val body  : String,
    val icon  : Int            // drawable resource ID
)


 val pages = listOf(
    OnboardPage(
        "Clone Your Phone",
        "Transfer all your data, apps, and settings from your old device to your new one seamlessly.",
        R.drawable.onboardingimg1           // 1st vector
    ),
    OnboardPage(
        "Smart AI Assistant",
        "Get intelligent help with device setup, file organization, and transfer optimization powered by AI.",
        R.drawable.onboardingimg1         // 2nd vector
    ),
    OnboardPage(
        "Ready in Minutes",
        "Enjoy your new phone with everything in place.",
        R.drawable.onboardingimg1                 // 3rd vector
    )
)