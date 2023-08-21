package com.udacity.project4.locationreminders.reminderslist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {

    override val viewModel: RemindersListViewModel by viewModel()

    private lateinit var binding: FragmentRemindersBinding
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_reminders,
            container, false
        )

        setDisplayHomeAsUpEnabled(false)

        setTitle(getString(R.string.app_name))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.clear_list -> {
                        clearList()
                        true
                    }

                    R.id.logout -> {
                        signOut()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadReminders()
            binding.refreshLayout.isRefreshing = false
        }
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
    }

    private fun clearList() {
        if (viewModel.remindersList.value.isNullOrEmpty()) return

        val geofenceIds = viewModel.remindersList.value!!.map { it.id }

        geofencingClient.removeGeofences(
            geofenceIds
        ).addOnSuccessListener {
            viewModel.clearList()
        }.addOnCanceledListener {
            Snackbar.make(
                binding.root,
                getString(R.string.operation_cancelled), Snackbar.LENGTH_SHORT
            ).show()
        }.addOnFailureListener {
            Snackbar.make(
                binding.root,
                getString(R.string.err_removing_geofences), Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        // use the navigationCommand live data to navigate between the fragments
        viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter(
            callBack = {
                val intent = Intent(context, ReminderDescriptionActivity::class.java)
                intent.putExtra(Constants.EXTRA_REMINDER_DATA_ITEM, it)
                startActivity(intent)
            },
            areItemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id },
            areContentsTheSame = { oldItem, newItem -> oldItem == newItem }
        )
        binding.reminderssRecyclerView.setup(adapter)
    }

    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this.requireContext())
            .addOnCompleteListener {
                navigateToAuthenticationActivity()
            }
    }

    private fun navigateToAuthenticationActivity() {
        val intent = Intent(this.requireActivity(), AuthenticationActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}