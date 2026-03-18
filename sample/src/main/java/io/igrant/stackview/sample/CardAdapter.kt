package io.igrant.stackview.sample

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.igrant.stackview.sample.databinding.ItemCardBinding

data class CardItem(
    val id: String,
    val title: String,
    val genre: String,
    val year: Int,
    val director: String,
    val rating: Float,
    val backgroundColor: Int
)

class CardAdapter(
    private val items: MutableList<CardItem>,
    private val onCardClicked: (Int) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(
        private val binding: ItemCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CardItem) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = "${item.genre} • ${item.year}"
            binding.tvLocation.text = "Dir. ${item.director} — ★ ${item.rating}"
            binding.root.setCardBackgroundColor(item.backgroundColor)

            val isLight = isColorLight(item.backgroundColor)
            val textColor = if (isLight) Color.parseColor("#333333") else Color.WHITE
            binding.tvTitle.setTextColor(textColor)
            binding.tvSubtitle.setTextColor(textColor)
            binding.tvLocation.setTextColor(textColor)

            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onCardClicked(pos)
                }
            }
        }

        private fun isColorLight(color: Int): Boolean {
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            return luminance > 0.5
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: CardItem) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, items.size)
    }

    fun updateItems(newItems: List<CardItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
