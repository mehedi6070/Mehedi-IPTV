package com.mehedi.iptv
import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class ChannelAdapter(private var items:List<Channel>,private val click:(Channel)->Unit):RecyclerView.Adapter<ChannelAdapter.VH>(){
 private var selected=-1
 fun submit(v:List<Channel>){items=v;notifyDataSetChanged()}
 fun setSelected(n:Int){selected=n;notifyDataSetChanged()}
 override fun onCreateViewHolder(p:ViewGroup,t:Int):VH{val v=TextView(p.context);v.layoutParams=RecyclerView.LayoutParams(-1,44);v.gravity=Gravity.CENTER_VERTICAL;v.setPadding(12,0,12,0);v.textSize=14f;return VH(v)}
 override fun onBindViewHolder(h:VH,pos:Int){val c=items[pos];h.t.text=String.format("%02d   %s",c.number,c.name);h.t.setTextColor(if(c.number==selected)Color.BLACK else Color.WHITE);h.t.setBackgroundColor(if(c.number==selected)Color.rgb(255,45,45) else Color.TRANSPARENT);h.t.setOnClickListener{click(c)};h.t.isFocusable=true;h.t.setOnFocusChangeListener{_,f->if(f&&c.number!=selected)h.t.setTextColor(Color.rgb(255,45,45))else h.t.setTextColor(if(c.number==selected)Color.BLACK else Color.WHITE)}}
 override fun getItemCount()=items.size
 class VH(val t:TextView):RecyclerView.ViewHolder(t)
}
