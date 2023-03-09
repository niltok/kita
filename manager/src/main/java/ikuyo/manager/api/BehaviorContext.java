package ikuyo.manager.api;

import io.reactivex.rxjava3.core.Observer;

public record BehaviorContext(Observer<Integer> render$, CommonContext common) {
}
