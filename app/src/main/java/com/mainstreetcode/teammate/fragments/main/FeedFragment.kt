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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar.Callback.DISMISS_EVENT_MANUAL
import com.google.android.material.snackbar.Snackbar.Callback.DISMISS_EVENT_SWIPE
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.feedAdapter
import com.mainstreetcode.teammate.adapters.viewholders.ChoiceBar
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder
import com.mainstreetcode.teammate.baseclasses.TeammatesBaseFragment
import com.mainstreetcode.teammate.model.Competitor
import com.mainstreetcode.teammate.model.Event
import com.mainstreetcode.teammate.model.JoinRequest
import com.mainstreetcode.teammate.model.ListState
import com.mainstreetcode.teammate.model.Media
import com.mainstreetcode.teammate.notifications.FeedItem
import com.mainstreetcode.teammate.notifications.isOf
import com.mainstreetcode.teammate.util.ScrollManager
import io.reactivex.Single
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Home screen
 */

class FeedFragment : TeammatesBaseFragment(R.layout.fragment_list_with_refresh) {

    private var onBoardingIndex: Int = 0
    private var isOnBoarding: Boolean = false

    override val showsFab: Boolean get() = !teamViewModel.isOnATeam

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        defaultUi(
                toolbarTitle = getString(R.string.home_greeting, timeOfDay, userViewModel.currentUser.firstName),
                fabText = R.string.team_search_create,
                fabIcon = R.drawable.ic_search_white_24dp,
                fabShows = false,
                bottomNavShows = true
        )

        val refreshAction = { disposables.add(feedViewModel.refresh(FeedItem::class.java).subscribe(this::onFeedUpdated, defaultErrorHandler::invoke)).let { Unit } }

        scrollManager = ScrollManager.with<RecyclerView.ViewHolder>(view.findViewById(R.id.list_layout))
                .withPlaceholder(EmptyViewHolder(view, R.drawable.ic_notifications_white_24dp, R.string.no_feed))
                .withAdapter(feedAdapter({ feedViewModel.getModelList(FeedItem::class.java) }, this::onFeedItemClicked))
                .withRefreshLayout(view.findViewById(R.id.refresh_layout), refreshAction)
                .addScrollListener { _, _ -> updateTopSpacerElevation() }
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withLinearLayoutManager()
                .build()
    }

    override fun onResume() {
        super.onResume()
        updateUi(fabShows = showsFab)
        scrollManager.setRefreshing()
        disposables.add(feedViewModel.refresh(FeedItem::class.java).subscribe(this::onFeedUpdated, defaultErrorHandler::invoke))
    }

    override fun togglePersistentUi() {
        super.togglePersistentUi()
        onBoard()
    }

    override fun onClick(view: View) = when (view.id) {
        R.id.fab -> navigator.push(TeamSearchFragment.newInstance()).let { Unit }
        else -> Unit
    }

    override fun augmentTransaction(transaction: FragmentTransaction, incomingFragment: Fragment) = when (incomingFragment) {
        is MediaDetailFragment ->
            transaction.listDetailTransition(MediaDetailFragment.ARG_MEDIA, incomingFragment, R.id.fragment_media_background, R.id.fragment_media_thumbnail)

        is JoinRequestFragment ->
            transaction.listDetailTransition(JoinRequestFragment.ARG_JOIN_REQUEST, incomingFragment)

        is EventEditFragment ->
            transaction.listDetailTransition(EventEditFragment.ARG_EVENT, incomingFragment)

        else -> super.augmentTransaction(transaction, incomingFragment)
    }

    private fun onFeedItemClicked(item: FeedItem<*>) {
        val context = context ?: return
        val builder = AlertDialog.Builder(context)

        item.isOf<Event>()?.apply {
            builder.setTitle(getString(R.string.attend_event))
                    .setPositiveButton(R.string.yes) { _, _ -> onFeedItemAction(feedViewModel.rsvpEvent(this, true)) }
                    .setNegativeButton(R.string.no) { _, _ -> onFeedItemAction(feedViewModel.rsvpEvent(this, false)) }
                    .setNeutralButton(R.string.event_details) { _, _ -> navigator.push(EventEditFragment.newInstance(model)) }
                    .show()
            return
        }

        item.isOf<Competitor>()?.apply {
            builder.setTitle(getString(R.string.accept_competition))
                    .setPositiveButton(R.string.yes) { _, _ -> onFeedItemAction(feedViewModel.processCompetitor(this, true)) }
                    .setNegativeButton(R.string.no) { _, _ -> onFeedItemAction(feedViewModel.processCompetitor(this, false)) }
                    .setNeutralButton(R.string.event_details) { _, _ ->
                        when {
                            !model.game.isEmpty -> GameFragment.newInstance(model.game)
                            !model.tournament.isEmpty -> TournamentDetailFragment.newInstance(model.tournament).pending(model)
                            else -> null
                        }?.let { navigator.push(it) }
                    }
                    .show()
            return
        }

        item.isOf<JoinRequest>()?.apply {
            val title = when {
                userViewModel.currentUser == model.user && model.isUserApproved -> getString(R.string.clarify_invitation, model.team.name)
                model.isTeamApproved -> getString(R.string.accept_invitation, model.team.name)
                else -> getString(R.string.add_user_to_team, model.user.firstName)
            }

            builder.setTitle(title)
                    .setPositiveButton(R.string.yes) { _, _ -> onFeedItemAction(feedViewModel.processJoinRequest(this, true)) }
                    .setNegativeButton(R.string.no) { _, _ -> onFeedItemAction(feedViewModel.processJoinRequest(this, false)) }
                    .setNeutralButton(R.string.event_details) { _, _ -> navigator.push(JoinRequestFragment.viewInstance(model)) }
                    .show()
            return
        }

        item.isOf<Media>()?.apply {
            updateUi(bottomNavShows = false)
            navigator.push(MediaDetailFragment.newInstance(model))
        }
    }

    private fun onFeedItemAction(diffResultSingle: Single<DiffUtil.DiffResult>) {
        transientBarDriver.toggleProgress(true)
        disposables.add(diffResultSingle.subscribe(this::onFeedUpdated, defaultErrorHandler::invoke))
    }

    private fun onFeedUpdated(diffResult: DiffUtil.DiffResult) {
        togglePersistentUi()
        transientBarDriver.toggleProgress(false)
        val isOnATeam = teamViewModel.isOnATeam
        scrollManager.onDiff(diffResult)
        feedViewModel.clearNotifications(FeedItem::class.java)
        scrollManager.updateForEmptyList(ListState(
                if (isOnATeam) R.drawable.ic_notifications_white_24dp else R.drawable.ic_group_black_24dp,
                if (isOnATeam) R.string.no_feed else R.string.no_team_feed))
    }

    private fun onBoard() {
        if (isOnBoarding || prefsViewModel.isOnBoarded || bottomSheetDriver.isBottomSheetShowing) return
        var prompts = listOf(*resources.getStringArray(R.array.on_boarding))
        prompts = prompts.subList(onBoardingIndex, prompts.size)

        isOnBoarding = true
        val iterator = prompts.iterator()
        val ref = AtomicReference<() -> Unit>()

        ref.set {
            transientBarDriver.showChoices { choiceBar ->
                choiceBar.setText(iterator.next())
                        .setPositiveText(getString(if (iterator.hasNext()) R.string.next else R.string.finish))
                        .setPositiveClickListener(View.OnClickListener {
                            onBoardingIndex++
                            if (iterator.hasNext()) ref.get().invoke()
                            else choiceBar.dismiss()
                        })
                        .addCallback(object : BaseTransientBottomBar.BaseCallback<ChoiceBar>() {
                            override fun onDismissed(bar: ChoiceBar?, event: Int) =
                                    onBoardDismissed(event)
                        })
            }
        }
        ref.get().invoke()
    }

    private fun onBoardDismissed(event: Int) {
        isOnBoarding = false
        if (event != DISMISS_EVENT_SWIPE && event != DISMISS_EVENT_MANUAL) return
        onBoardingIndex = 0
        prefsViewModel.isOnBoarded = true
    }

    companion object {

        fun newInstance(): FeedFragment = FeedFragment().apply { arguments = Bundle() }

        private val timeOfDay: String
            get() {
                val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                return if (hourOfDay in 1..11) "morning"
                else "evening"
            }
    }
}