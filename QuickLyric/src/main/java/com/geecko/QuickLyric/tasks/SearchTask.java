/*
 * *
 *  * This file is part of QuickLyric
 *  * Copyright © 2017 QuickLyric SPRL
 *  *
 *  * QuickLyric is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * QuickLyric is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License
 *  * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.geecko.QuickLyric.tasks;

import android.os.AsyncTask;
import android.os.Process;
import android.widget.Toast;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.SearchActivity;
import com.geecko.QuickLyric.fragment.SearchFragment;
import com.geecko.QuickLyric.provider.Genius;
import com.geecko.QuickLyric.provider.JLyric;
import com.geecko.QuickLyric.provider.LyricWiki;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import java.util.ArrayList;
import java.util.List;

public class SearchTask extends AsyncTask<Object, Object, List<Lyrics>> {

    private SearchFragment searchFragment;
    private String searchQuery;

    @Override
    @SuppressWarnings("unchecked")
    protected List<Lyrics> doInBackground(Object... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        searchQuery = (String) params[0];
        searchFragment = (SearchFragment) params[1];
        int position = (Integer) params[2];
        SearchActivity searchActivity = (SearchActivity) searchFragment.getActivity();
        if (searchActivity == null)
            return null;

        List<Lyrics> results;
        do
            results = doSearch(searchActivity.searchProviders.get(position));
        while (results == null && !isCancelled() &&  searchFragment != null &&
                searchFragment.getActivity() != null &&
                (OnlineAccessVerifier.check(searchFragment.getActivity()) ||
                position == 0)); // DatabaseHelper

        return results;
    }

    protected void onPostExecute(List<Lyrics> results) {
        if (searchFragment.getActivity() == null)
            return;
        ((SearchActivity) searchFragment.getActivity()).setSearchQuery(searchQuery);
        if (results == null && searchFragment.getActivity() != null &&
                !OnlineAccessVerifier.check(searchFragment.getActivity()))
            Toast.makeText(searchFragment.getActivity(),
                    searchFragment.getString(R.string.connection_error),
                    Toast.LENGTH_LONG).show();
        else if (searchFragment.getActivity() != null) {
            if (results == null)
                results = new ArrayList<>(0);
            searchFragment.setResults(results);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Lyrics> doSearch(Class provider) {
        switch (provider.getSimpleName()) {
            case "LyricWiki":
                return LyricWiki.search(searchQuery);
            case "Genius":
                return Genius.search(searchQuery);
            case "DatabaseHelper":
                return DatabaseHelper.getInstance(searchFragment.getActivity()).search(searchQuery);
            case "JLyric":
                return JLyric.search(searchQuery);
            default:
                try {
                    return (List<Lyrics>) provider.getMethod("search", String.class).invoke(null, searchQuery);
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                    return null;
                }
        }
    }

}
