/*
 * MIT License
 *
 * Copyright (c) 2019 Adetunji Dahunsi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mainstreetcode.teammate.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.Shell
import com.mainstreetcode.teammate.adapters.UserHostListener
import com.mainstreetcode.teammate.adapters.teamMemberAdapter
import com.mainstreetcode.teammate.baseclasses.TeammatesBaseFragment
import com.mainstreetcode.teammate.baseclasses.removeSharedElementTransitions
import com.mainstreetcode.teammate.model.JoinRequest
import com.mainstreetcode.teammate.model.Role
import com.mainstreetcode.teammate.model.Team
import com.mainstreetcode.teammate.model.User
import com.mainstreetcode.teammate.util.ScrollManager
import com.tunjid.androidx.recyclerview.diff.Differentiable

/**
 * Displays a [team&#39;s][Team] [members][User].
 */

class TeamMembersFragment : TeammatesBaseFragment(R.layout.fragment_list_with_refresh),
        UserHostListener {

    private lateinit var team: Team
    private lateinit var teamModels: List<Differentiable>

    override val showsFab: Boolean get() = targetRequestCode == 0 && roleScopeViewModel.hasPrivilegedRole(team)

    private val toolbarTitle: CharSequence get() = if (targetFragment != null) "" else getString(R.string.team_name_prefix, team.name)

    override val stableTag: String
        get() {
            val superResult = super.stableTag
            val tempTeam = arguments!!.getParcelable<Team>(ARG_TEAM)

            return if (tempTeam == null) superResult else superResult + "-" + tempTeam.hashCode()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        team = arguments!!.getParcelable(ARG_TEAM)!!
        teamModels = teamMemberViewModel.getModelList(team)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        defaultUi(
                toolbarTitle = toolbarTitle,
                toolBarMenu = R.menu.fragment_team_detail,
                fabIcon = R.drawable.ic_group_add_white_24dp,
                fabText = R.string.invite_user,
                fabShows = showsFab
        )
        val refreshAction = {
            disposables.add(teamMemberViewModel.refresh(team)
                    .subscribe(this::onTeamMembersUpdated, defaultErrorHandler::invoke)).let { Unit }
        }

        scrollManager = ScrollManager.with<RecyclerView.ViewHolder>(view.findViewById(R.id.list_layout))
                .withRefreshLayout(view.findViewById(R.id.refresh_layout), refreshAction)
                .withAdapter(teamMemberAdapter(::teamModels, this))
                .addScrollListener { _, dy -> updateFabForScrollState(dy) }
                .addScrollListener { _, _ -> updateTopSpacerElevation() }
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withStaggeredGridLayoutManager(2)
                .build()
    }

    override fun onResume() {
        super.onResume()
        fetchTeamMembers(true)
        watchForRoleChanges(team) { updateUi(fabShows = showsFab) }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val visible = showsFab

        val editItem = menu.findItem(R.id.action_edit)
        val deleteItem = menu.findItem(R.id.action_delete)
        val blockedItem = menu.findItem(R.id.action_blocked)
        val tournamentItem = menu.findItem(R.id.action_team_tournaments)

        editItem?.isVisible = visible
        deleteItem?.isVisible = visible
        blockedItem?.isVisible = visible
        tournamentItem?.isVisible = team.sport.supportsCompetitions()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_edit -> navigator.push(TeamEditFragment.newEditInstance(team))

        R.id.action_team_tournaments -> navigator.push(TournamentsFragment.newInstance(team))

        R.id.action_blocked -> navigator.push(BlockedUsersFragment.newInstance(team))

        R.id.action_delete -> AlertDialog.Builder(requireContext()).setTitle(getString(R.string.delete_team_prompt, team.name))
                .setMessage(R.string.delete_team_prompt_body)
                .setPositiveButton(R.string.yes) { _, _ -> deleteTeam() }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .show().let { true }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onRoleClicked(role: Role) {
        val target = targetFragment
        val canPick = target is Shell.UserAdapterListener

        if (canPick) (target as Shell.UserAdapterListener).onUserClicked(role.user)
        else navigator.push(RoleEditFragment.newInstance(role))
    }

    override fun onJoinRequestClicked(request: JoinRequest) {
        val target = targetFragment
        val canPick = target is Shell.UserAdapterListener

        if (canPick) transientBarDriver.showSnackBar(getString(R.string.stat_user_not_on_team))
        else navigator.push(JoinRequestFragment.viewInstance(request))
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.fab -> if (roleScopeViewModel.hasPrivilegedRole(team))
                navigator.push(JoinRequestFragment.inviteInstance(team))
        }
    }

    @SuppressLint("CommitTransaction")
    override fun augmentTransaction(transaction: FragmentTransaction, incomingFragment: Fragment) = when (incomingFragment) {
        is RoleEditFragment ->
            transaction.listDetailTransition(RoleEditFragment.ARG_ROLE, incomingFragment)

        is JoinRequestFragment ->
            transaction.listDetailTransition(JoinRequestFragment.ARG_JOIN_REQUEST, incomingFragment)

        else -> super.augmentTransaction(transaction, incomingFragment)
    }

    private fun fetchTeamMembers(fetchLatest: Boolean) {
        if (fetchLatest) scrollManager.setRefreshing()
        else transientBarDriver.toggleProgress(true)

        disposables.add(teamMemberViewModel.getMany(team, fetchLatest).subscribe(this::onTeamMembersUpdated, defaultErrorHandler::invoke))
    }

    private fun deleteTeam() {
        disposables.add(teamViewModel.deleteTeam(team).subscribe({ onTeamDeleted() }, defaultErrorHandler::invoke))
    }

    private fun onTeamMembersUpdated(diffResult: DiffUtil.DiffResult) {
        scrollManager.onDiff(diffResult)
        updateUi(toolbarInvalidated = true)
    }

    private fun onTeamDeleted() {
        transientBarDriver.showSnackBar(getString(R.string.deleted_team, team.name))
        removeSharedElementTransitions()

        activity?.onBackPressed()
    }

    companion object {

        private const val ARG_TEAM = "team"

        fun newInstance(team: Team): TeamMembersFragment = TeamMembersFragment().apply { arguments = bundleOf(ARG_TEAM to team) }
    }
}
