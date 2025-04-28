package com.example.testemptyapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import android.util.Log
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.unit.Velocity
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

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
    val nestedScrollConnection = rememberNestedScrollInteropConnection()
    val listState = rememberLazyListState()
    val scrollDispatcher = remember { NestedScrollDispatcher() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection, scrollDispatcher),
        flingBehavior = CustomFlingBehavior(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        repeat(40) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 0.dp, vertical = 8.dp)
                ) {
                    Text("TEST", fontSize = 14.sp, modifier = Modifier.padding(15.dp))
                }
            }
        }
    }
}

@Composable
fun CustomFlingBehavior(): FlingBehavior {
    val flingDecay = rememberSplineBasedDecay<Float>()
    // Create a standard fling behavior as a fallback
    val standardFling = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior()

    return remember(flingDecay, standardFling) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                Log.d("CustomFlingBehavior", "______________performFling______________")
                Log.d("CustomFlingBehavior", "performFling called with initialVelocity: $initialVelocity")
                // Ignore very small velocities to prevent erratic behavior
                if (abs(initialVelocity) < 10f) {
                    Log.d("CustomFlingBehavior", "Ignoring small velocity: $initialVelocity")
                    return 0f
                }

                val initialDirection = sign(initialVelocity)
                var totalConsumed = 0f
                val vel = initialVelocity

                // Use the standard spline-based decay for natural Android physics
                val animationState = AnimationState(
                    initialValue = 0f,
                    initialVelocity = vel
                )

                var customFlingSucceeded = false
                try {
                    Log.d("CustomFlingBehavior", "Starting animation with initialDirection: $initialDirection")

                    animationState.animateDecay(flingDecay) {
                        // Calculate delta from the start of the animation
                      //  val delta = value - totalConsumed
                        val delta = value - totalConsumed
                        Log.d("CustomFlingBehavior", "Animation frame - delta: $delta, value: $value, totalConsumed: $totalConsumed")

                        // Ensure delta direction matches initial velocity direction
                        // This prevents erratic behavior when the animation produces values
                        // in the opposite direction of the initial gesture
                        if (sign(delta) == initialDirection || abs(delta) < 0.5f) {
                            val consumed = scrollBy(delta)
                            totalConsumed += consumed
                            Log.d("CustomFlingBehavior", "Scrolled by delta: $delta, consumed: $consumed, new totalConsumed: $totalConsumed")

                            if (consumed != 0f) {
                                customFlingSucceeded = true
                            }

                            // If we couldn't scroll as expected, stop the animation
                            if (abs(delta - consumed) > 0.5f) {
                                Log.d("CustomFlingBehavior", "Canceling animation - couldn't scroll as expected")
                                cancelAnimation()
                            }
                        } else {
                            // If delta is in wrong direction, cancel the animation
                            Log.d("CustomFlingBehavior", "Canceling animation - delta in wrong direction: ${sign(delta)} vs expected: $initialDirection")
                            cancelAnimation()
                        }
                    }
                } catch (e: CancellationException) {
                    // Animation was canceled, which is fine
                    Log.d("CustomFlingBehavior", "Animation was canceled: ${e.message}")
                }

                // If our custom implementation didn't handle the fling properly, use the standard fling behavior
                if (!customFlingSucceeded) {
                    Log.d("CustomFlingBehavior", "Custom fling failed, trying standard fling behavior")
                    try {
                        with(standardFling) {
                            return this@performFling.performFling(vel)
                        }
                    } catch (e: Exception) {
                        Log.e("CustomFlingBehavior", "Error using standard fling behavior: ${e.message}")
                    }
                }

                // Return a reduced velocity to parent to prevent over-scrolling
                // This helps with the coordination between Compose and AppBarLayout
                val returnVelocity = animationState.velocity// initialDirection * min(abs(animationState.velocity), 2000f) * 0.3f
                Log.d("CustomFlingBehavior", "Returning velocity: $returnVelocity (original: ${animationState.velocity})")
                return returnVelocity
            }
        }
    }
}