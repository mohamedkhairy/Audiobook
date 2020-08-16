package com.example.audiobook.aboutbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.borjabravo.readmoretextview.ReadMoreTextView
import com.example.audiobook.main.MainActivity
import com.example.audiobook.R
import com.example.audiobook.adapters.ChapterAdapter
import com.example.audiobook.model.ListenHubAudioBooks
import com.example.audiobook.utils.BOOK_DATA
import com.example.audiobook.utils.loadAsyncImage

class AboutBookFragment : Fragment(), View.OnClickListener {

    lateinit var title: TextView
    lateinit var author: TextView
    lateinit var genres: TextView
    lateinit var duration: TextView
    lateinit var description: ReadMoreTextView
    lateinit var bookCover: ImageView
    lateinit var playBook: FrameLayout
    lateinit var chapterRecyclerView: RecyclerView
    lateinit var chapterAdapter: ChapterAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iniViews(view)
        setListener()
        prepareRecyclerView()
        fillView()
    }

    private fun getBookData(): ListenHubAudioBooks? =
        arguments?.getParcelable<ListenHubAudioBooks>(BOOK_DATA) as ListenHubAudioBooks


    private fun iniViews(view: View) {
        with(view) {
            title = findViewById(R.id.book_title)
            author = findViewById(R.id.author)
            genres = findViewById(R.id.genres)
            duration = findViewById(R.id.duration)
            description = findViewById(R.id.about)
            bookCover = findViewById(R.id.about_book_cover)
            chapterRecyclerView = findViewById(R.id.chapters_recycler_view)
            playBook = findViewById(R.id.playbookfram)
        }
    }

    private fun fillView() {
        getBookData()?.let {
            title.text = it.title
            author.text = it.authors[0].firstName
            genres.text = it.genres
            duration.text = it.totaltime
            description.text = it.description
            bookCover.loadAsyncImage(it.imageUrl)

            chapterAdapter.apply {
                addChapter(it.chapters)
                notifyDataSetChanged()
            }
        }
    }

    private fun setListener() {
        description.setOnClickListener(this)
        bookCover.setOnClickListener(this)
        playBook.setOnClickListener(this)
    }

    private fun prepareRecyclerView() {
        with(chapterRecyclerView) {
            layoutManager = LinearLayoutManager(context)
            chapterAdapter = ChapterAdapter(
                mutableListOf(),
                getBookData()?.imageUrl
            ) { goToPlayer(it.toString()) }
            adapter = chapterAdapter
            (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true

        }
    }

    override fun onClick(v: View?) {
        val id = v?.id
        when (id) {
            R.id.about_book_cover, R.id.playbookfram -> {
                goToPlayer()
            }
        }
    }


    private fun goToPlayer(chapterIndex: String = "0") {
        (activity as? MainActivity)?.let {
            it.goToPlayer(chapterIndex, getBookData())
            getBookData()?.let { it1 -> it.mainViewModel.updateNowPlaying(it1 , chapterIndex) }
        }
    }

    companion object {

        const val TAG = "AboutBookFragment"

    }
}