package com.udacity.project4.locationreminders.reminderslist

import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter

class RemindersListAdapter(
    callBack: (selectedReminder: ReminderDataItem) -> Unit,
    areItemsTheSame: (oldItem: ReminderDataItem, newItem: ReminderDataItem) -> Boolean,
    areContentsTheSame: (oldItem: ReminderDataItem, newItem: ReminderDataItem) -> Boolean
) :
    BaseRecyclerViewAdapter<ReminderDataItem>(callBack, areItemsTheSame, areContentsTheSame) {

    override fun getLayoutRes(viewType: Int) = R.layout.it_reminder
}