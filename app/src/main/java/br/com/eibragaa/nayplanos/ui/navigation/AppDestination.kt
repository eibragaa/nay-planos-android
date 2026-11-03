package br.com.eibragaa.nayplanos.ui.navigation

import androidx.annotation.StringRes
import br.com.eibragaa.nayplanos.R

enum class AppDestination(
    @StringRes val labelRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
) {
    HOME(R.string.nav_home, R.string.home_title, R.string.home_description),
    NEW_PLAN(R.string.nav_new_plan, R.string.new_plan_title, R.string.new_plan_description),
    HISTORY(R.string.nav_history, R.string.history_title, R.string.history_description),
    SETTINGS(R.string.nav_settings, R.string.settings_title, R.string.settings_description),
}
