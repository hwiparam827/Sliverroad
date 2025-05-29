package com.example.sliverroad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sliverroad.data.DeliveryHistory

class HistoryAdapter(
    private var items: List<DeliveryHistory>
) : RecyclerView.Adapter<HistoryAdapter.HistoryVH>() {

    inner class HistoryVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRoute = view.findViewById<TextView>(R.id.tvRoute)
        private val tvType  = view.findViewById<TextView>(R.id.tvType)
        private val tvFee   = view.findViewById<TextView>(R.id.tvFee)

        fun bind(item: DeliveryHistory) {
            tvRoute.text = "${item.fromLocation}  ›  ${item.toLocation}"
            tvType.text  = item.packageType
            tvFee.text   = "배송료 %,d원".format(item.fee)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryVH(view)
    }

    override fun onBindViewHolder(holder: HistoryVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /** 새 데이터를 바인딩하려면 이 메서드 호출 */
    fun submitList(newList: List<DeliveryHistory>) {
        items = newList
        notifyDataSetChanged()
    }
}
