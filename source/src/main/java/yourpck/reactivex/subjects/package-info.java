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
 * Classes representing so-called hot sources, aka subjects, that implement a base reactive class and
 * the respective consumer type at once to allow forms of multicasting events to multiple
 * consumers as well as consuming another base reactive type of their kind.
 * <p>
 * Available subject classes with their respective base classes and consumer interfaces:
 * <br>
 * <table border="1" style="border-collapse: collapse;" summary="The available subject classes with their respective base classes and consumer interfaces.">
 * <tr><td><b>Subject type</b></td><td><b>Base class</b></td><td><b>Consumer interface</b></td></tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.Subject Subject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.AsyncSubject AsyncSubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.BehaviorSubject BehaviorSubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.PublishSubject PublishSubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.ReplaySubject ReplaySubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.UnicastSubject UnicastSubject}
 * </td>
 * <td>{@link yourpck.reactivex.Observable Observable}</td>
 * <td>{@link yourpck.reactivex.Observer Observer}</td>
 * </tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.SingleSubject SingleSubject}</td>
 * <td>{@link yourpck.reactivex.Single Single}</td>
 * <td>{@link yourpck.reactivex.SingleObserver SingleObserver}</td>
 * </tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.MaybeSubject MaybeSubject}</td>
 * <td>{@link yourpck.reactivex.Maybe Maybe}</td>
 * <td>{@link yourpck.reactivex.MaybeObserver MaybeObserver}</td>
 * </tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.CompletableSubject CompletableSubject}</td>
 * <td>{@link yourpck.reactivex.Completable Completable}</td>
 * <td>{@link yourpck.reactivex.CompletableObserver CompletableObserver}</td>
 * </tr>
 * </table>
 * <p>
 * The backpressure-aware variants of the {@code Subject} class are called
 * {@link yourpck.org.reactivestreams.Processor}s and reside in the {@code yourpck.reactivex.processors} package.
 *
 * @see yourpck.reactivex.processors
 * <p>
 * Classes representing so-called hot sources, aka subjects, that implement a base reactive class and
 * the respective consumer type at once to allow forms of multicasting events to multiple
 * consumers as well as consuming another base reactive type of their kind.
 * <p>
 * Available subject classes with their respective base classes and consumer interfaces:
 * <br>
 * <table border="1" style="border-collapse: collapse;" summary="The available subject classes with their respective base classes and consumer interfaces.">
 * <tr><td><b>Subject type</b></td><td><b>Base class</b></td><td><b>Consumer interface</b></td></tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.Subject Subject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.AsyncSubject AsyncSubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.BehaviorSubject BehaviorSubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.PublishSubject PublishSubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.ReplaySubject ReplaySubject}
 * <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.UnicastSubject UnicastSubject}
 * </td>
 * <td>{@link yourpck.reactivex.Observable Observable}</td>
 * <td>{@link yourpck.reactivex.Observer Observer}</td>
 * </tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.SingleSubject SingleSubject}</td>
 * <td>{@link yourpck.reactivex.Single Single}</td>
 * <td>{@link yourpck.reactivex.SingleObserver SingleObserver}</td>
 * </tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.MaybeSubject MaybeSubject}</td>
 * <td>{@link yourpck.reactivex.Maybe Maybe}</td>
 * <td>{@link yourpck.reactivex.MaybeObserver MaybeObserver}</td>
 * </tr>
 * <tr>
 * <td>{@link yourpck.reactivex.subjects.CompletableSubject CompletableSubject}</td>
 * <td>{@link yourpck.reactivex.Completable Completable}</td>
 * <td>{@link yourpck.reactivex.CompletableObserver CompletableObserver}</td>
 * </tr>
 * </table>
 * <p>
 * The backpressure-aware variants of the {@code Subject} class are called
 * {@link yourpck.org.reactivestreams.Processor}s and reside in the {@code yourpck.reactivex.processors} package.
 * @see yourpck.reactivex.processors
 */

/**
 * Classes representing so-called hot sources, aka subjects, that implement a base reactive class and
 * the respective consumer type at once to allow forms of multicasting events to multiple
 * consumers as well as consuming another base reactive type of their kind.
 * <p>
 * Available subject classes with their respective base classes and consumer interfaces:
 * <br>
 * <table border="1" style="border-collapse: collapse;" summary="The available subject classes with their respective base classes and consumer interfaces.">
 * <tr><td><b>Subject type</b></td><td><b>Base class</b></td><td><b>Consumer interface</b></td></tr>
 * <tr>
 *     <td>{@link yourpck.reactivex.subjects.Subject Subject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.AsyncSubject AsyncSubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.BehaviorSubject BehaviorSubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.PublishSubject PublishSubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.ReplaySubject ReplaySubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link yourpck.reactivex.subjects.UnicastSubject UnicastSubject}
 *     </td>
 *     <td>{@link yourpck.reactivex.Observable Observable}</td>
 *     <td>{@link yourpck.reactivex.Observer Observer}</td>
 * </tr>
 * <tr>
 *     <td>{@link yourpck.reactivex.subjects.SingleSubject SingleSubject}</td>
 *     <td>{@link yourpck.reactivex.Single Single}</td>
 *     <td>{@link yourpck.reactivex.SingleObserver SingleObserver}</td>
 * </tr>
 * <tr>
 *     <td>{@link yourpck.reactivex.subjects.MaybeSubject MaybeSubject}</td>
 *     <td>{@link yourpck.reactivex.Maybe Maybe}</td>
 *     <td>{@link yourpck.reactivex.MaybeObserver MaybeObserver}</td>
 * </tr>
 * <tr>
 *     <td>{@link yourpck.reactivex.subjects.CompletableSubject CompletableSubject}</td>
 *     <td>{@link yourpck.reactivex.Completable Completable}</td>
 *     <td>{@link yourpck.reactivex.CompletableObserver CompletableObserver}</td>
 * </tr>
 * </table>
 * <p>
 * The backpressure-aware variants of the {@code Subject} class are called
 * {@link yourpck.org.reactivestreams.Processor}s and reside in the {@code yourpck.reactivex.processors} package.
 * @see yourpck.reactivex.processors
 */
package yourpck.reactivex.subjects;
