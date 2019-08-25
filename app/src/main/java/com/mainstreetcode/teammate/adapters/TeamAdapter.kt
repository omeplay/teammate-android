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

package com.mainstreetcode.teammate.adapters

import android.view.ViewGroup
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.viewholders.AdViewHolder
import com.mainstreetcode.teammate.adapters.viewholders.ContentAdViewHolder
import com.mainstreetcode.teammate.adapters.viewholders.InstallAdViewHolder
import com.mainstreetcode.teammate.adapters.viewholders.TeamViewHolder
import com.mainstreetcode.teammate.adapters.viewholders.bind
import com.mainstreetcode.teammate.model.Ad
import com.mainstreetcode.teammate.model.Role
import com.mainstreetcode.teammate.model.Team
import com.mainstreetcode.teammate.util.CONTENT_AD
import com.mainstreetcode.teammate.util.INSTALL_AD
import com.mainstreetcode.teammate.util.TEAM
import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable

/**
 * Adapter for [Team]
 */

open class TeamAdapter(
        private val items: List<Differentiable>,
        listener: AdapterListener
) : InteractiveAdapter<InteractiveViewHolder<*>, TeamAdapter.AdapterListener>(listener) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): InteractiveViewHolder<*> = when (viewType) {
        CONTENT_AD -> ContentAdViewHolder(getItemView(R.layout.viewholder_grid_content_ad, viewGroup), adapterListener)
        INSTALL_AD -> InstallAdViewHolder(getItemView(R.layout.viewholder_grid_install_ad, viewGroup), adapterListener)
        else -> TeamViewHolder(getItemView(R.layout.viewholder_grid_item, viewGroup), adapterListener)
    }

    override fun onBindViewHolder(viewHolder: InteractiveViewHolder<*>, position: Int) {
        when (val item = items[position]) {
            is Ad<*> -> (viewHolder as AdViewHolder<*>).bind(item)
            is Team -> (viewHolder as TeamViewHolder).bind(item)
            is Role -> (viewHolder as TeamViewHolder).bind(item.team)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

    override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
        is Team -> TEAM
        is Role -> TEAM
        else -> (item as Ad<*>).type
    }

    interface AdapterListener : InteractiveAdapter.AdapterListener {
        fun onTeamClicked(item: Team)

        companion object {
            fun asSAM(function: (Team) -> Unit) = object : AdapterListener {
                override fun onTeamClicked(item: Team) = function.invoke(item)
            }
        }
    }

}
