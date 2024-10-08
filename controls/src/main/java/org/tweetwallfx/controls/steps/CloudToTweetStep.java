/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2024 TweetWallFX
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tweetwallfx.controls.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.tweetwallfx.controls.TweetLayout;
import org.tweetwallfx.controls.TweetWordNodeFactory;
import org.tweetwallfx.controls.Word;
import org.tweetwallfx.controls.WordleSkin;
import org.tweetwallfx.emoji.control.EmojiFlow;
import org.tweetwallfx.stepengine.api.DataProvider;
import org.tweetwallfx.stepengine.api.Step;
import org.tweetwallfx.stepengine.api.StepEngine.MachineContext;
import org.tweetwallfx.stepengine.api.config.StepEngineSettings;
import org.tweetwallfx.stepengine.dataproviders.PhotoImageMediaEntryDataProvider;
import org.tweetwallfx.stepengine.dataproviders.TweetDataProvider;
import org.tweetwallfx.stepengine.dataproviders.TweetUserProfileImageDataProvider;
import org.tweetwallfx.transitions.FontSizeTransition;
import org.tweetwallfx.transitions.LocationTransition;
import org.tweetwallfx.tweet.api.Tweet;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class CloudToTweetStep implements Step {

    private CloudToTweetStep() {
        // prevent external instantiation
    }

    @Override
    public boolean shouldSkip(MachineContext context) {
        return null == context.getDataProvider(TweetDataProvider.class).getTweet();
    }

    @Override
    public java.time.Duration preferredStepDuration(final MachineContext context) {
        return java.time.Duration.ofSeconds(5);
    }

    @Override
    public void doStep(final MachineContext context) {
        WordleSkin wordleSkin = (WordleSkin) context.get("WordleSkin");
        Bounds layoutBounds = wordleSkin.getPane().getLayoutBounds();
        Tweet displayTweet = context.getDataProvider(TweetDataProvider.class).getTweet();
        Tweet originalTweet = displayTweet.getOriginTweet();

        Point2D minPosTweetText = new Point2D(layoutBounds.getWidth() / 6d, (layoutBounds.getHeight() - wordleSkin.getLogo().getImage().getHeight()) / 4d);

        double width = layoutBounds.getWidth() * (2 / 3d);

        TweetLayout tweetLayout = TweetLayout.createTweetLayout(new TweetLayout.Configuration(originalTweet, wordleSkin.getFont(), wordleSkin.getTweetFontSize()));

        List<Transition> fadeOutTransitions = new ArrayList<>();
        List<Transition> moveTransitions = new ArrayList<>();
        List<Transition> fadeInTransitions = new ArrayList<>();

        Point2D lowerLeft = new Point2D(minPosTweetText.getX(), minPosTweetText.getY());
        Point2D tweetLineOffset = new Point2D(0, 0);

        Duration defaultDuration = Duration.seconds(1.5);

        TweetWordNodeFactory wordNodeFactory = TweetWordNodeFactory.createFactory(new TweetWordNodeFactory.Config(wordleSkin.getFont(), wordleSkin.getTweetFontSize()));

        for (TweetLayout.TweetWord tweetWord : tweetLayout.getWordLayoutInfo()) {
            Word word = new Word(tweetWord.text.trim(), -2);
            if (wordleSkin.word2TextMap.containsKey(word)) {
                Text textNode = wordleSkin.word2TextMap.remove(word);
                wordleSkin.tweetWordList.add(new TweetLayout.TweetWordNode(tweetWord, textNode));

                moveTransitions.add(new FontSizeTransition(defaultDuration, textNode)
                        .withSize(textNode.getFont().getSize(), wordleSkin.getTweetFontSize()));

                Bounds bounds = tweetLayout.getWordLayoutInfo().stream().filter(tw -> tw.text.trim().equals(word.getText())).findFirst().get().bounds;

                tweetLineOffset = tweetLayout.tweetWordLineOffset(bounds, lowerLeft, width, tweetLineOffset);
                Point2D twPoint = tweetLayout.layoutTweetWord(bounds, minPosTweetText, tweetLineOffset);
                if (twPoint.getY() > lowerLeft.getY()) {
                    lowerLeft = lowerLeft.add(0, twPoint.getY() - lowerLeft.getY());
                }
                moveTransitions.add(new LocationTransition(defaultDuration, textNode)
                        .withX(textNode.getLayoutX(), twPoint.getX())
                        .withY(textNode.getLayoutY(), twPoint.getY()));
            } else {
                Text textNode = wordNodeFactory.createTextNode(word.getText());

                wordNodeFactory.fontSizeAdaption(textNode, wordleSkin.getTweetFontSize());
                wordleSkin.tweetWordList.add(new TweetLayout.TweetWordNode(tweetWord, textNode));

                Bounds bounds = tweetWord.bounds;

                tweetLineOffset = tweetLayout.tweetWordLineOffset(bounds, lowerLeft, width, tweetLineOffset);
                Point2D twPoint = tweetLayout.layoutTweetWord(bounds, minPosTweetText, tweetLineOffset);

                textNode.setLayoutX(twPoint.getX());
                textNode.setLayoutY(twPoint.getY());
                if (twPoint.getY() > lowerLeft.getY()) {
                    lowerLeft = lowerLeft.add(0, twPoint.getY() - lowerLeft.getY());
                }
                wordleSkin.getPane().getChildren().add(textNode);
                fadeInTransitions.add(addFadeTransition(defaultDuration, textNode, 0, 1));
            }
        }

        // kill the remaining words from the cloud
        wordleSkin.word2TextMap.entrySet().forEach(entry -> {
            Text textNode = entry.getValue();
            FadeTransition ft = new FadeTransition(defaultDuration, textNode);
            ft.setToValue(0);
            ft.setOnFinished(event
                    -> wordleSkin.getPane().getChildren().remove(textNode));
            fadeOutTransitions.add(ft);
        });
        wordleSkin.word2TextMap.clear();

        // layout image and meta data first
        Pane infoBox = createInfoBox(wordleSkin, context, displayTweet, lowerLeft);
        infoBox.setId("tweetInfo");
        wordleSkin.setInfoBox(infoBox);
        wordleSkin.getPane().getChildren().add(infoBox);
        // add fade in for image and meta data
        fadeInTransitions.add(addFadeTransition(defaultDuration, infoBox, 0, 1));

        Pane mediaBox = createMediaBox(wordleSkin, context, displayTweet);
        wordleSkin.setMediaBox(mediaBox);
        if (null != mediaBox) {
            wordleSkin.getPane().getChildren().add(mediaBox);
            // add fade in for media
            fadeInTransitions.add(addFadeTransition(defaultDuration, mediaBox, 0, 1));
        }

        ParallelTransition fadeOuts = new ParallelTransition();
        ParallelTransition moves = new ParallelTransition();
        ParallelTransition fadeIns = new ParallelTransition();
        fadeIns.getChildren().addAll(fadeInTransitions);
        moves.getChildren().addAll(moveTransitions);
        fadeOuts.getChildren().addAll(fadeOutTransitions);
        SequentialTransition morph = new SequentialTransition(fadeOuts, moves, fadeIns);

        morph.setOnFinished(e -> context.proceed());
        morph.play();
    }

    private FadeTransition addFadeTransition(final Duration duration, final Node node, final double initialOpacity, final double targetOpacity) {
        FadeTransition ft = new FadeTransition(duration, node);
        node.setOpacity(initialOpacity);
        ft.setToValue(targetOpacity);
        return ft;
    }

    private Pane createMediaBox(
            final WordleSkin wordleSkin,
            final MachineContext context,
            final Tweet displayTweet) {
        final Tweet originalTweet = displayTweet.getOriginTweet();

        if (originalTweet.getMediaEntries().length > 0) {
            HBox mediaBox = new HBox(10);
            mediaBox.setOpacity(0);
            mediaBox.setPadding(new Insets(10));
            mediaBox.setAlignment(Pos.CENTER_RIGHT);
            mediaBox.layoutXProperty().bind(Bindings.add(
                    wordleSkin.getInfoBox().getPadding().getBottom() + wordleSkin.getInfoBox().getPadding().getTop(),
                    wordleSkin.getLogo().getImage().widthProperty()));
            mediaBox.layoutYProperty().bind(Bindings.add(
                    wordleSkin.getInfoBox().heightProperty(),
                    wordleSkin.getInfoBox().layoutYProperty()));
            // ensure media box fills the complete area, so that layouting from right to left works
            mediaBox.minWidthProperty().bind(Bindings.add(
                    wordleSkin.getPane().widthProperty(),
                    Bindings.negate(Bindings.add(
                            80,
                            wordleSkin.getLogo().getImage().widthProperty()
                    ))));
            mediaBox.minHeightProperty().bind(Bindings.max(
                    50,
                    Bindings.add(
                            wordleSkin.getPane().heightProperty(),
                            Bindings.negate(mediaBox.layoutYProperty()))));
            // MaxSize = MinSize
            mediaBox.maxWidthProperty().bind(mediaBox.minWidthProperty());
            mediaBox.maxHeightProperty().bind(mediaBox.minHeightProperty());

            final int imageCount = Math.min(3, originalTweet.getMediaEntries().length);   //limit to maximum loading time of 3 images.
            final PhotoImageMediaEntryDataProvider pimedp = context.getDataProvider(PhotoImageMediaEntryDataProvider.class);

            for (int i = 0; i < imageCount; i++) {
                Image mediaImage = pimedp.getImage(originalTweet.getMediaEntries()[i]);
                ImageView mediaView = new ImageView(mediaImage);
                mediaView.setPreserveRatio(true);
                mediaView.setCache(true);
                mediaView.setSmooth(true);
                mediaView.fitWidthProperty().bind(Bindings.divide(
                        Bindings.add(
                                mediaBox.maxWidthProperty(),
                                -10),
                        imageCount
                ));
                mediaView.fitHeightProperty().bind(Bindings.add(
                        mediaBox.maxHeightProperty(),
                        -10));
                mediaBox.getChildren().add(mediaView);
            }

            return mediaBox;
        } else {
            return null;
        }
    }

    private Pane createInfoBox(
            final WordleSkin wordleSkin,
            final MachineContext context,
            final Tweet displayTweet,
            final Point2D lowerLeft) {
        final Tweet originalTweet = displayTweet.getOriginTweet();

        Image profileImage = context.getDataProvider(TweetUserProfileImageDataProvider.class).getImage(originalTweet.getUser());
        ImageView imageView = new ImageView(profileImage);
        Rectangle clip = new Rectangle(64, 64);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imageView.setClip(clip);

        HBox imageBox = new HBox(imageView);
        imageBox.setPadding(new Insets(10));

        Label name = new Label(originalTweet.getUser().getName());
        name.getStyleClass().setAll("name");

        Label handle = new Label("@" + originalTweet.getUser().getScreenName() + " - " + wordleSkin.getDf().format(originalTweet.getCreatedAt()));
        handle.getStyleClass().setAll("handle");

        HBox firstLineBox = new HBox();
        HBox secondLineBox = new HBox(name);
        HBox thirdLineBox = new HBox(handle);

        if (originalTweet.getUser().isVerified()) {
            EmojiFlow verifiedIcon = new EmojiFlow("✅", 24D, 24D);
            verifiedIcon.getStyleClass().addAll("verifiedAccount");

            secondLineBox.getChildren().add(verifiedIcon);
            HBox.setMargin(verifiedIcon, new Insets(9, 10, 0, 5));
        }

        if (displayTweet.isRetweet()) {
            EmojiFlow retweetIcon = new EmojiFlow("🔁", 22D, 22D);
            retweetIcon.getStyleClass().addAll("retweetBack");

            Label retweetName = new Label(displayTweet.getUser().getName());
            retweetName.getStyleClass().setAll("retweetName");

            firstLineBox.getChildren().addAll(retweetIcon, retweetName);
            HBox.setMargin(retweetIcon, new Insets(0, 10, 0, 0));
        }

        if (wordleSkin.getFavIconsVisible()) {
            if (0 < originalTweet.getRetweetCount()) {
                EmojiFlow faiReTwCount = new EmojiFlow("🔁", 24D, 24D);
                faiReTwCount.getStyleClass().setAll("retweetCount");

                Label reTwCount = new Label(String.valueOf(originalTweet.getRetweetCount()));
                reTwCount.getStyleClass().setAll("handle");

                thirdLineBox.getChildren().addAll(faiReTwCount, reTwCount);
                HBox.setMargin(faiReTwCount, new Insets(5, 10, 0, 5));
            }

            if (0 < originalTweet.getFavoriteCount()) {
                EmojiFlow faiFavCount = new EmojiFlow("👍", 24D, 24D);
                faiFavCount.getStyleClass().setAll("favoriteCount");

                Label favCount = new Label(String.valueOf(originalTweet.getFavoriteCount()));
                favCount.getStyleClass().setAll("handle");

                thirdLineBox.getChildren().addAll(faiFavCount, favCount);
                HBox.setMargin(faiFavCount, new Insets(5, 10, 0, 5));
            }
        }

        final GridPane infoBox = new GridPane();

        infoBox.setStyle("-fx-padding: 20px;");
        infoBox.setPrefHeight(100);
        infoBox.setMaxHeight(100);
        infoBox.setLayoutX(lowerLeft.getX());
        infoBox.setLayoutY(lowerLeft.getY());
        infoBox.setAlignment(Pos.CENTER);
        infoBox.getChildren().addAll(imageBox, firstLineBox, secondLineBox, thirdLineBox);

        GridPane.setConstraints(imageBox, 0, 1, 1, 2);
        GridPane.setConstraints(firstLineBox, 1, 0);
        GridPane.setConstraints(secondLineBox, 1, 1);
        GridPane.setConstraints(thirdLineBox, 1, 2);

        return infoBox;
    }

    /**
     * Implementation of {@link Step.Factory} as Service implementation creating
     * {@link CloudToTweetStep}.
     */
    public static final class FactoryImpl implements Step.Factory {

        @Override
        public CloudToTweetStep create(final StepEngineSettings.StepDefinition stepDefinition) {
            return new CloudToTweetStep();
        }

        @Override
        public Class<CloudToTweetStep> getStepClass() {
            return CloudToTweetStep.class;
        }

        @Override
        public Collection<Class<? extends DataProvider>> getRequiredDataProviders(final StepEngineSettings.StepDefinition stepSettings) {
            return Arrays.asList(
                    TweetDataProvider.class,
                    PhotoImageMediaEntryDataProvider.class,
                    TweetUserProfileImageDataProvider.class);
        }
    }
}
