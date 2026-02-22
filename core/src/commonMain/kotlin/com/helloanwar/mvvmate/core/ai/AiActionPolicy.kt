package com.helloanwar.mvvmate.core.ai

import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState

/**
 * A security policy interface that determines whether an AI/LLM is allowed
 * to execute a specific [UiAction] given the current [UiState].
 * 
 * By default, you should treat LLMs as untrusted clients. Use this policy
 * to explicitly whitelist read-only or safe actions while blocking destructive
 * actions (like "Delete Account" or "Make Payment") unless explicitly 
 * verified by the user.
 *
 * @param S The type of UI state.
 * @param A The type of user actions.
 */
interface AiActionPolicy<S : UiState, A : UiAction> {
    
    /**
     * Evaluates if the proposed [action] is safe to execute in the [currentState].
     * 
     * @param action The action the AI is attempting to dispatch.
     * @param currentState The immediate state of the ViewModel.
     * @return `true` if the action is allowed, `false` otherwise.
     */
    fun isActionAllowed(action: A, currentState: S): Boolean
}

/**
 * A highly restrictive default policy that blocks all AI actions.
 * Ideal as a fallback or starting point.
 */
class DenyAllPolicy<S : UiState, A : UiAction> : AiActionPolicy<S, A> {
    override fun isActionAllowed(action: A, currentState: S): Boolean = false
}

/**
 * An unrestrictive policy that allows the AI to execute any action.
 * **WARNING**: Do not use this in production if your ViewModel contains
 * actions that mutate sensitive user data, perform purchases, or modify
 * authentication state.
 */
class AllowAllPolicy<S : UiState, A : UiAction> : AiActionPolicy<S, A> {
    override fun isActionAllowed(action: A, currentState: S): Boolean = true
}
