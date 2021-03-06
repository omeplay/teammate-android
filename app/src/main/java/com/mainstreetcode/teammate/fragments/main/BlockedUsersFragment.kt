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

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.blockedUserAdapter
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder
import com.mainstreetcode.teammate.baseclasses.TeammatesBaseFragment
import com.mainstreetcode.teammate.model.BlockedUser
import com.mainstreetcode.teammate.model.Team
import com.mainstreetcode.teammate.util.ScrollManager
import com.tunjid.androidx.recyclerview.diff.Differentiable

/**
 * Lists [events][BlockedUser]
 */

class BlockedUsersFragment : TeammatesBaseFragment(R.layout.fragment_list_with_refresh) {

    private lateinit var team: Team
    private lateinit var items: List<Differentiable>

    override val stableTag: String
        get() {
            val superResult = super.stableTag
            val tempTeam = arguments!!.getParcelable<Team>(ARG_TEAM)

            return if (tempTeam != null) superResult + "-" + tempTeam.hashCode()
            else superResult
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        team = arguments!!.getParcelable(ARG_TEAM)!!
        items = blockedUserViewModel.getModelList(team)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        defaultUi(
                toolbarTitle = getString(R.string.blocked_users_title, team.name),
                toolBarMenu = R.menu.fragment_events,
                fabShows = showsFab
        )

        val refreshAction = {
            disposables.add(blockedUserViewModel.refresh(team)
                    .subscribe(this::onBlockedUsersUpdated, defaultErrorHandler::invoke)).let { Unit }
        }

        scrollManager = ScrollManager.with<RecyclerView.ViewHolder>(view.findViewById(R.id.list_layout))
                .withPlaceholder(EmptyViewHolder(view, R.drawable.ic_block_white_24dp, R.string.no_blocked_users))
                .withRefreshLayout(view.findViewById(R.id.refresh_layout), refreshAction)
                .withEndlessScroll { fetchBlockedUsers(false) }
                .addScrollListener { _, _ -> updateTopSpacerElevation() }
                .withAdapter(blockedUserAdapter(items, this::onBlockedUserClicked))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withGridLayoutManager(2)
                .build()
    }

    override fun onResume() {
        super.onResume()
        fetchBlockedUsers(true)
    }

    override fun augmentTransaction(transaction: FragmentTransaction, incomingFragment: Fragment) = when (incomingFragment) {
        is BlockedUserViewFragment ->
            transaction.listDetailTransition(BlockedUserViewFragment.ARG_BLOCKED_USER, incomingFragment)

        else -> super.augmentTransaction(transaction, incomingFragment)
    }

    private fun onBlockedUserClicked(blockedUser: BlockedUser) {
        navigator.push(BlockedUserViewFragment.newInstance(blockedUser))
    }

    private fun fetchBlockedUsers(fetchLatest: Boolean) {
        if (fetchLatest) scrollManager.setRefreshing()
        else transientBarDriver.toggleProgress(true)

        disposables.add(blockedUserViewModel.getMany(team, fetchLatest)
                .subscribe(this::onBlockedUsersUpdated, defaultErrorHandler::invoke))
    }

    private fun onBlockedUsersUpdated(result: DiffUtil.DiffResult) {
        scrollManager.onDiff(result)
        transientBarDriver.toggleProgress(false)
    }

    companion object {

        private const val ARG_TEAM = "team"

        fun newInstance(team: Team): BlockedUsersFragment = BlockedUsersFragment().apply { arguments = bundleOf(ARG_TEAM to team) }
    }
}
