/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Classes representing so-called hot backpressure-aware sources, aka <strong>processors</strong>,
 * that implement the {@link FlowableProcessor} class,
 * the Reactive Streams {@link yourpck.org.reactivestreams.Processor Processor} interface
 * to allow forms of multicasting events to one or more subscribers as well as consuming another
 * Reactive Streams {@link yourpck.org.reactivestreams.Publisher Publisher}.
 * <p>
 * Available processor implementations:
 * <br>
 * <ul>
 * <li>{@link yourpck.reactivex.processors.AsyncProcessor AsyncProcessor} - replays the very last item</li>
 * <li>{@link yourpck.reactivex.processors.BehaviorProcessor BehaviorProcessor} - remembers the latest item</li>
 * <li>{@link yourpck.reactivex.processors.MulticastProcessor MulticastProcessor} - coordinates its source with its consumers</li>
 * <li>{@link yourpck.reactivex.processors.PublishProcessor PublishProcessor} - dispatches items to current consumers</li>
 * <li>{@link yourpck.reactivex.processors.ReplayProcessor ReplayProcessor} - remembers some or all items and replays them to consumers</li>
 * <li>{@link yourpck.reactivex.processors.UnicastProcessor UnicastProcessor} - remembers or relays items to a single consumer</li>
 * </ul>
 * <p>
 * The non-backpressured variants of the {@code FlowableProcessor} class are called
 * {@link yourpck.reactivex.Subject}s and reside in the {@code yourpck.reactivex.subjects} package.
 *
 * @see yourpck.reactivex.subjects
 * <p>
 * Classes representing so-called hot backpressure-aware sources, aka <strong>processors</strong>,
 * that implement the {@link FlowableProcessor} class,
 * the Reactive Streams {@link yourpck.org.reactivestreams.Processor Processor} interface
 * to allow forms of multicasting events to one or more subscribers as well as consuming another
 * Reactive Streams {@link yourpck.org.reactivestreams.Publisher Publisher}.
 * <p>
 * Available processor implementations:
 * <br>
 * <ul>
 * <li>{@link yourpck.reactivex.processors.AsyncProcessor AsyncProcessor} - replays the very last item</li>
 * <li>{@link yourpck.reactivex.processors.BehaviorProcessor BehaviorProcessor} - remembers the latest item</li>
 * <li>{@link yourpck.reactivex.processors.MulticastProcessor MulticastProcessor} - coordinates its source with its consumers</li>
 * <li>{@link yourpck.reactivex.processors.PublishProcessor PublishProcessor} - dispatches items to current consumers</li>
 * <li>{@link yourpck.reactivex.processors.ReplayProcessor ReplayProcessor} - remembers some or all items and replays them to consumers</li>
 * <li>{@link yourpck.reactivex.processors.UnicastProcessor UnicastProcessor} - remembers or relays items to a single consumer</li>
 * </ul>
 * <p>
 * The non-backpressured variants of the {@code FlowableProcessor} class are called
 * {@link yourpck.reactivex.Subject}s and reside in the {@code yourpck.reactivex.subjects} package.
 * @see yourpck.reactivex.subjects
 */

/**
 * Classes representing so-called hot backpressure-aware sources, aka <strong>processors</strong>,
 * that implement the {@link FlowableProcessor} class,
 * the Reactive Streams {@link yourpck.org.reactivestreams.Processor Processor} interface
 * to allow forms of multicasting events to one or more subscribers as well as consuming another
 * Reactive Streams {@link yourpck.org.reactivestreams.Publisher Publisher}.
 * <p>
 * Available processor implementations:
 * <br>
 * <ul>
 *     <li>{@link yourpck.reactivex.processors.AsyncProcessor AsyncProcessor} - replays the very last item</li>
 *     <li>{@link yourpck.reactivex.processors.BehaviorProcessor BehaviorProcessor} - remembers the latest item</li>
 *     <li>{@link yourpck.reactivex.processors.MulticastProcessor MulticastProcessor} - coordinates its source with its consumers</li>
 *     <li>{@link yourpck.reactivex.processors.PublishProcessor PublishProcessor} - dispatches items to current consumers</li>
 *     <li>{@link yourpck.reactivex.processors.ReplayProcessor ReplayProcessor} - remembers some or all items and replays them to consumers</li>
 *     <li>{@link yourpck.reactivex.processors.UnicastProcessor UnicastProcessor} - remembers or relays items to a single consumer</li>
 * </ul>
 * <p>
 * The non-backpressured variants of the {@code FlowableProcessor} class are called
 * {@link yourpck.reactivex.Subject}s and reside in the {@code yourpck.reactivex.subjects} package.
 * @see yourpck.reactivex.subjects
 */
package yourpck.reactivex.processors;
