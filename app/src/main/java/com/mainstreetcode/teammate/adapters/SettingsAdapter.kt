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

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.viewholders.SettingsViewHolder
import com.mainstreetcode.teammate.model.SettingsItem
import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter


class SettingsAdapter(
        private val items: List<SettingsItem>,
        listener: SettingsAdapterListener
) : InteractiveAdapter<SettingsViewHolder, SettingsAdapter.SettingsAdapterListener>(listener) {

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): SettingsViewHolder {
        val context = viewGroup.context
        val itemView = LayoutInflater.from(context).inflate(R.layout.viewholder_settings, viewGroup, false)
        return SettingsViewHolder(itemView, adapterListener)
    }

    override fun onBindViewHolder(settingsViewHolder: SettingsViewHolder, i: Int) {
        settingsViewHolder.bind(items[i])
    }

    override fun getItemCount(): Int = items.size

    interface SettingsAdapterListener : AdapterListener {
        fun onSettingsItemClicked(item: SettingsItem)
    }

}
