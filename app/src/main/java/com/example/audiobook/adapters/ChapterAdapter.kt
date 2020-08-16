package com.example.audiobook.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.audiobook.R
import com.example.audiobook.model.Chapter
import com.example.audiobook.utils.loadAsyncImage
import kotlinx.android.synthetic.main.chapter_item.view.*

class ChapterAdapter(
    val chapters: MutableList<Chapter>,
    val imageUrl: String?,
    val clickListener: (Int) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.DataViewHolder>() {

    inner class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(chapter: Chapter, position: Int) {
            itemView.apply {
                chapter_title.text = chapter.title
                narrator.text = chapter.narratedBy
                chapter_duration.text = chapter.duration
                chapter_cover.loadAsyncImage(imageUrl)
                setOnClickListener { clickListener(position) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder =
        DataViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.chapter_item, parent, false)
        )

    override fun getItemCount(): Int = chapters.size

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        holder.bind(chapters[position], position)
    }

    fun addChapter(list: List<Chapter>) {
        this.chapters.apply {
            clear()
            addAll(list)
        }

    }
}