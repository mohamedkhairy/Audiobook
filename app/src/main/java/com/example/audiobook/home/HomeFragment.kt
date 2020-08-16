package com.example.audiobook.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.audiobook.R
import com.example.audiobook.aboutbook.AboutBookFragment
import com.example.audiobook.adapters.HomeAdapter
import com.example.audiobook.model.Book
import com.example.audiobook.model.ListenHubAudioBooks
import com.example.audiobook.utils.BOOK_DATA
import com.example.audiobook.utils.Result
import com.example.audiobook.utils.addFragment

class HomeFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var emptyMessage: TextView
    private lateinit var homeAdapter: HomeAdapter


    val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this).get(HomeViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iniViews(view)
        prepareRecyclerView()
        setupObservers()
        addOnRefreshListener()
    }

    private fun addOnRefreshListener() {
        swipeRefresh.setOnRefreshListener {
            setupObservers()
        }
    }

    private fun iniViews(view: View) {
        with(view) {
            recyclerView = findViewById(R.id.recycler_view)
            swipeRefresh = findViewById(R.id.swipe_refresh)
            emptyView = findViewById(R.id.empty_no_connection)
            emptyMessage = findViewById(R.id.empty_list_message)

        }
    }

    private fun prepareRecyclerView() {
        with(recyclerView) {
            layoutManager = GridLayoutManager(activity, 2, LinearLayoutManager.VERTICAL, false)
            homeAdapter = HomeAdapter(mutableListOf()) { goToBook(it) }
            adapter = homeAdapter
            (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true

        }
    }

    private fun goToBook(book: ListenHubAudioBooks) {
        val aboutBookFragment = AboutBookFragment()
        val bundle = Bundle()
        bundle.putParcelable(BOOK_DATA, book)
        aboutBookFragment.arguments = bundle
        (activity as? AppCompatActivity)?.let {
            it.addFragment(lazy {
                aboutBookFragment
            }, AboutBookFragment.TAG)
        }
    }

    private fun setupObservers() {
        showLoading()
        homeViewModel.getBooks().observe(this, Observer {
            hideLoading()
            it?.let { result ->
                when (result) {
                    is Result.Success<*> -> {
                        val books = result.t as Book
                        val xcv = books.listenHubAudioBooks[0].title
                        handleRightSide(books)
                    }
                    is Result.Error -> {
                        handleLiftSide()
                    }
                }
            }
        })
    }


    private fun handleRightSide(book: Book) {
        hideErrorMessage()
        homeAdapter.apply {
            addBookList(book.listenHubAudioBooks)
            notifyDataSetChanged()
            recyclerView.startLayoutAnimation()
        }

    }

    private fun handleLiftSide() {
        showErrorMessage()

    }

    private fun showErrorMessage() {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }

    private fun hideErrorMessage() {
        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
    }

    private fun hideLoading() {
        swipeRefresh.isRefreshing = false
    }

    private fun showLoading() {
        swipeRefresh.isRefreshing = true
    }

    companion object {

        const val TAG = "HomeFragment"

    }
}