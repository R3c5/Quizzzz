/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package client;


import client.scenes.*;
import client.utils.LongPollingUtils;
import client.utils.WebSocketsUtils;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

public class MyModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(MainCtrl.class).in(Scopes.SINGLETON);
        binder.bind(MultiplayerCtrl.class).in(Scopes.SINGLETON);
        binder.bind(SplashCtrl.class).in(Scopes.SINGLETON);
        binder.bind(WaitingAreaCtrl.class).in(Scopes.SINGLETON);
        binder.bind(SingleplayerCtrl.class).in(Scopes.SINGLETON);
        binder.bind(LeaderBoardCtrl.class).in(Scopes.SINGLETON);
        binder.bind(RoomSelectionCtrl.class).in(Scopes.SINGLETON);
        binder.bind(WebViewCtrl.class).in(Scopes.SINGLETON);
        binder.bind(GamemodeCtrl.class).in(Scopes.SINGLETON);
        binder.bind(SurvivalCtrl.class).in(Scopes.SINGLETON);
        binder.bind(TimeAttackCtrl.class).in(Scopes.SINGLETON);
        binder.bind(TutorialScreenCtrl.class).in(Scopes.SINGLETON);
        binder.bind(PodiumCtrl.class).in(Scopes.SINGLETON);
        binder.bind(EndGameScreenCtrl.class).in(Scopes.SINGLETON);

        binder.bind(LongPollingUtils.class).in(Scopes.SINGLETON);
        binder.bind(WebSocketsUtils.class).in(Scopes.SINGLETON);

        binder.bind(GameAnimation.class).in(Scopes.SINGLETON);
        binder.bind(SoundManager.class).in(Scopes.SINGLETON);
    }
}