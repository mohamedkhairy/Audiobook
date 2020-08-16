package com.example.audiobook.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.audiobook.R
import com.example.audiobook.model.ListenHubAudioBooks
import com.example.audiobook.utils.loadAsyncImage
import kotlinx.android.synthetic.main.book_card_item.view.*

class HomeAdapter(
    val bookList: MutableList<ListenHubAudioBooks>,
    val clickListener: (ListenHubAudioBooks) -> Unit
) : RecyclerView.Adapter<HomeAdapter.DataViewHolder>() {

    inner class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(book: ListenHubAudioBooks) {
            itemView.apply {
                book_title.text = book.title
                author.text = book.authors[0].firstName

                book_cover.loadAsyncImage(book.imageUrl)

            }
            itemView.setOnClickListener { clickListener(book) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder =
        DataViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.book_card_item, parent, false)
        )

    override fun getItemCount(): Int = bookList.size

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        holder.bind(bookList[position])
    }

    fun addBookList(list: List<ListenHubAudioBooks>) {
        this.bookList.apply {
            clear()
            addAll(list)
        }

    }
}