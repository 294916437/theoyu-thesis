import React from "react";
import MeetingRoom from "./src/MeetingRoom";
import {AuthScreen, MainScreen, SplashScreen} from "./src/screens";
import {useBlueSkyApp} from "./src/useBlueSkyApp";

function App(): React.JSX.Element {
  const {booting, auth, main, actions} = useBlueSkyApp();

  if (booting) {
    return <SplashScreen />;
  }

  if (!auth.authenticated) {
    return <AuthScreen state={auth} actions={actions} />;
  }

  return (
    <MainScreen
      state={main}
      actions={actions}
      room={<MeetingRoom roomStateJson={JSON.stringify(main.activeRoom)} onAction={actions.handleRoomAction} />}
    />
  );
}

export default App;
