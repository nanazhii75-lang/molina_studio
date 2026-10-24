package com.molina.cinema

import com.molina.cinema.databinding.DialogPlaylistBinding
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

internal class PlaylistDialog(private val player: MPVView) {
    private lateinit var binding: DialogPlaylistBinding

    private var playlist = listOf<MPVView.PlaylistItem>()
    private var selectedIndex = -1

    interface Listeners {
        fun pickFile()
        fun openUrl()
        fun onItemPicked(item: MPVView.PlaylistItem)
    }

    var listeners: Listeners? = null

    fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogPlaylistBinding.inflate(layoutInflater)

        // Set up recycler view
        binding.list.adapter = CustomAdapter(this)
        binding.list.setHasFixedSize(true)
        refresh()

        binding.fileBtn.setOnClickListener { listeners?.pickFile() }
        binding.urlBtn.setOnClickListener { listeners?.openUrl() }

        binding.shuffleBtn.setOnClickListener {
            player.changeShuffle(true)
            refresh()
        }
        binding.repeatBtn.setOnClickListener {
            player.cycleRepeat()
            refresh()
        }

        Utils.handleInsetsAsPadding(binding.root)
        return binding.root
    }

    fun refresh() {
        selectedIndex = MPVLib.getPropertyInt("playlist-pos") ?: -1
        playlist = player.loadPlaylist()
        MpvLog.v(TAG, "PlaylistDialog: loaded ${playlist.size} items")
        binding.list.adapter!!.notifyDataSetChanged()
        binding.list.scrollToPosition(playlist.indexOfFirst { it.index == selectedIndex })

        /*
         * At least on api 33 there is in some cases a (reproducible) bug, where the space below the
         * recycler view for the two buttons is not taken into account and they go out-of-bounds of the
         * alert dialog. This fixes it.
         */
        binding.list.post {
            binding.list.parent.requestLayout()
        }

        val accent = ContextCompat.getColor(binding.root.context, R.color.player_playlist_active)
        val normal = ContextCompat.getColor(binding.root.context, R.color.player_dialog_text)
        val disabled = ContextCompat.getColor(binding.root.context, R.color.alpha_disabled)
        //
        val shuffleState = player.getShuffle()
        binding.shuffleBtn.apply {
            isEnabled = playlist.size > 1
            imageTintList = if (isEnabled)
                ColorStateList.valueOf(if (shuffleState) accent else normal)
            else
                ColorStateList.valueOf(disabled)
        }
        val repeatState = player.getRepeat()
        binding.repeatBtn.apply {
            imageTintList = ColorStateList.valueOf(if (repeatState > 0) accent else normal)
            setImageResource(if (repeatState == 2) R.drawable.ic_repeat_one_24dp else R.drawable.ic_repeat_24dp)
        }
    }

    private fun clickItem(position: Int) {
        val item = playlist[position]
        listeners?.onItemPicked(item)
    }

    class CustomAdapter(private val parent: PlaylistDialog) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        class ViewHolder(private val parent: PlaylistDialog, view: View) :
            RecyclerView.ViewHolder(view) {
            private val textView: TextView

            init {
                textView = view.findViewById(android.R.id.text1)
                view.setOnClickListener {
                    parent.clickItem(bindingAdapterPosition)
                }
            }

            fun bind(item: MPVView.PlaylistItem, selected: Boolean) {
                textView.text = item.title ?: Utils.fileBasename(item.filename)
                textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
                textView.setTextColor(ContextCompat.getColor(textView.context,
                    if (selected) R.color.player_playlist_active else R.color.player_dialog_text))
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.dialog_playlist_item, viewGroup, false)
            return ViewHolder(parent, view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val item = parent.playlist[position]
            viewHolder.bind(item, item.index == parent.selectedIndex)
        }

        override fun getItemCount() = parent.playlist.size
    }

    companion object {
        private const val TAG = "mpv"
    }
}
