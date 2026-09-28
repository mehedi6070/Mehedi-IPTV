package site.mehedi.iptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChannelAdapter(
    private var items: List<Channel>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private var selected = 0

    fun submitList(newItems: List<Channel>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelected(position: Int) {
        val old = selected
        selected = position
        notifyItemChanged(old)
        notifyItemChanged(selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.text.text = "%03d   %s".format(position + 1, c.name)
        holder.text.setBackgroundResource(
            if (position == selected) R.drawable.selected_bg else R.drawable.panel_bg
        )
        holder.text.setOnClickListener { onClick(position) }
        holder.text.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) onClick(position)
        }
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.channelName)
    }
}
