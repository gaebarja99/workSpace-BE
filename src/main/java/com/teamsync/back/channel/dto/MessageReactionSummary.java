package com.teamsync.back.channel.dto;

import com.teamsync.back.channel.message.MessageReaction;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FR-202-B(메시지 이모지 반응) 집계 응답. emoji별로 count를 세고, 현재 사용자가 그 이모지로 반응했는지(reactedByMe)를 담는다.
 */
public record MessageReactionSummary(
		String emoji,
		int count,
		boolean reactedByMe
) {
	/**
	 * 한 메시지의 반응 목록을 emoji별로 묶어 집계한다. 이모지의 첫 등장 순서(입력 컬렉션 순서)를 보존한다.
	 * currentUserId가 null이면 reactedByMe는 항상 false다.
	 */
	public static List<MessageReactionSummary> summarize(Collection<MessageReaction> reactions, Long currentUserId) {
		Map<String, int[]> counts = new LinkedHashMap<>();
		for (MessageReaction reaction : reactions) {
			int[] agg = counts.computeIfAbsent(reaction.getEmoji(), key -> new int[] {0, 0});
			agg[0]++;
			if (currentUserId != null && reaction.getUser().getId().equals(currentUserId)) {
				agg[1] = 1;
			}
		}
		return counts.entrySet().stream()
				.map(entry -> new MessageReactionSummary(entry.getKey(), entry.getValue()[0], entry.getValue()[1] == 1))
				.toList();
	}
}
