package com.gamebooster.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gamebooster.R
import com.gamebooster.util.GameInfo

class GameAdapter(
    private val games: List<GameInfo>,
    private val onGameClick: (GameInfo) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameIcon: ImageView = view.findViewById(R.id.gameIcon)
        val gameName: TextView = view.findViewById(R.id.gameName)
        val boostIndicator: View = view.findViewById(R.id.boostIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]

        holder.gameName.text = game.name
        game.icon?.let { holder.gameIcon.setImageDrawable(it) }

        holder.itemView.setOnClickListener {
            onGameClick(game)
        }

        // Add entrance animation
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .start()
    }

    override fun getItemCount() = games.size
}
