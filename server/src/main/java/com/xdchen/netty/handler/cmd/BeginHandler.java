package com.xdchen.netty.handler.cmd;

import com.alibaba.fastjson.JSONObject;
import com.xdchen.netty.exception.BusiException;
import com.xdchen.netty.handler.cmd.GameHandler;
import com.xdchen.netty.model.*;
import com.xdchen.netty.server.CardServerInitializer;
import com.xdchen.netty.service.CardService;
import com.xdchen.netty.service.ICardService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class BeginHandler implements GameHandler {
    private ICardService cardService = new CardService();
    private Room room;
    public BeginHandler(CardServerInitializer serverInitializer) {
        this.room = serverInitializer.getRoom();
    }

    @Override
    public void execute(GameRequest paramGameRequest, GameResponse paramGameResponse) {
        Channel master = room.getMaster();
        if (!room.isCanStart()) {
            throw new BusiException("不能开始游戏");
        }
        if (paramGameRequest.getChannel() != master) {
            throw new BusiException("不是房主，不能启动游戏");
        }
        room.setStart(true);

        List<Card> totalCards = cardService.initCards();
        cardService.shuffleCards(totalCards);
        List<Card> cards = cardService.dealCards(room.getUserCards(), totalCards);
        room.getUserCards().get(room.getUsernameByChannel(master)).addAll(cards);
        int[] countArray = new int[3];
        for (int i = 0; i < room.getChannelList().size(); i++) {
            countArray[i] = room.getUserCards().get(room.getBeginUsers()[i].getUsername()).size();
        }
        for (int i = 0; i < 3; i++) {
            Channel channel = room.getChannelList().get(i);
            String username = room.getBeginUsers()[i].getUsername();
            JSONObject data = new JSONObject();
            int leftCount = i == 0 ? countArray[2] : countArray[i - 1];
            int rightCount = i == 2 ? countArray[0] : countArray[i + 1];
            data.put("leftCount", leftCount);
            data.put("rightCount", rightCount);
            data.put("cards", room.getUserCards().get(username));
            paramGameResponse.setRetData(data);
            channel.writeAndFlush(new TextWebSocketFrame(paramGameResponse.getResponseString()));
        }
    }
}
