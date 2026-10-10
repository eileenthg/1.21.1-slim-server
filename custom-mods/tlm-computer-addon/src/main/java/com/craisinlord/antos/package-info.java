/**
 * Vendored copy of AntOS (https://github.com/craisinlord/AntOS, common-1.21.1), kept in its own package so it can be
 * diffed against upstream. The repository has no published licence, so this copy is for private use only.
 * <p>
 * Differences from upstream:
 * <ul>
 *     <li>Removed the computer block, block entity, item, renderer and model, plus the Antroid phone. The TLM maid
 *     computer is the only device, wired up by {@code com.github.eileen.workingmaidcomputers}.</li>
 *     <li>{@code AnternetAccountHandler} no longer tracks block devices or pending disk insertions; accounts are
 *     bound to the player, so the workspace does not need a block entity.</li>
 *     <li>{@code TaskObjectiveApi.record} takes a {@code ComputerWorkspace}.</li>
 *     <li>Added built-in games under {@code content.client.game.mini} and {@code content.minigame}: Coin Miner,
 *     Paddle Ball (ported from MrCrayfish's Furniture Mod: Refurbished, MIT), Tic-Tac-Toe, Minesweeper and Checkers.
 *     {@code ComputerGame.Session} gained {@code hover} and {@code mouseClicked}.</li>
 * </ul>
 */
package com.craisinlord.antos;
