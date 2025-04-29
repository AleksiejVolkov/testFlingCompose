package com.example.testemptyapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContentView(R.layout.main_activity)
        supportFragmentManager.beginTransaction()
            .add(
                R.id.compose_bottom_sheet_placeholder,
                CompleteProfileDialogFragment(),
                "CompleteProfileDialog"
            )
            .commit()
    }
}

class CompleteProfileDialogFragment : androidx.fragment.app.Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CleanScreen()
            }
        }
}

@Composable
fun CleanScreen() {
    // todo not ideal version - there is a bug of scrolling items "under" top bar while app bar is collapsing.
    //  Could be fix by blocking list scroll while appbar is collapsing. Also need to fix AppBar collapse trigger percentage
    val nestedScrollConnection = rememberNestedScrollInteropConnection()
    val dispatcher = remember { NestedScrollDispatcher() }
    val listState = rememberLazyListState()
    val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    val scrollConnection = remember {
        object : NestedScrollConnection {
            // BEFORE child scrolls
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y

                if (dy > 0
                    && listState.firstVisibleItemIndex != 0
                    && listState.firstVisibleItemScrollOffset != 0
                ) {
                    return dispatcher.dispatchPreScroll(available, source)  // consume it all
                }
                val nestedScrollAvailable = available.copy(y = available.y * POWER_MULTIPLIER)
                nestedScrollConnection.onPreScroll(nestedScrollAvailable, source)
                return dispatcher.dispatchPreScroll(available, source)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return dispatcher.dispatchPostScroll(consumed, available, source)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y > 0
                    && listState.firstVisibleItemIndex != 0
                    && listState.firstVisibleItemScrollOffset != 0
                ) {
                    return super.onPreFling(available)  // consume it all
                }
                nestedScrollConnection.onPreFling(available)
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                nestedScrollConnection.onPostFling(consumed, available)
                return super.onPostFling(consumed, available)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(list) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 0.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Text("TEST", fontSize = 14.sp, modifier = Modifier.padding(15.dp))
            }
        }
    }
}

private const val POWER_MULTIPLIER = 1.3f