package com.example.sliverroad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sliverroad.data.DeliveryHistoryItem  // 새 데이터 모델로 교체

class HistoryAdapter(
    private var items: List<DeliveryHistoryItem>
) : RecyclerView.Adapter<HistoryAdapter.HistoryVH>() {

    inner class HistoryVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRoute = view.findViewById<TextView>(R.id.tvRoute)
        private val tvType  = view.findViewById<TextView>(R.id.tvType)
        private val tvFee   = view.findViewById<TextView>(R.id.tvFee)

        fun bind(item: DeliveryHistoryItem) {
            tvRoute.text = "${item.pickup_location}  ›  ${item.delivery_address}"
            tvType.text  = item.item_type
            tvFee.text   = "배송 완료"  // 서버 응답에 금액 없음 → 상태 표시로 대체
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

    fun submitList(newList: List<DeliveryHistoryItem>) {
        items = newList
        notifyDataSetChanged()
    }
}
