package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.ui.Screen

class DealFinderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_deal_finder)

            // Intent for Featured Card -> Deals / Flyers
            val dealsIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("route", Screen.Deals.route)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val dealsPendingIntent = PendingIntent.getActivity(
                context, 101, dealsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_featured_card, dealsPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, dealsPendingIntent)

            // Intent for Search / Compare
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("route", Screen.Search.route)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context, 102, searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_search, searchPendingIntent)

            // Intent for Shopping List
            val listIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("route", Screen.Lists.route)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val listPendingIntent = PendingIntent.getActivity(
                context, 103, listIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_list, listPendingIntent)

            // Intent for Loyalty Cards Wallet
            val loyaltyIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("route", Screen.LoyaltyCards.route)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val loyaltyPendingIntent = PendingIntent.getActivity(
                context, 104, loyaltyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_loyalty, loyaltyPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
