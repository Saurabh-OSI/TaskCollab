import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom"; // ✅ ADDED
import { DragDropContext, Draggable, Droppable } from "@hello-pangea/dnd";

import API from "../services/api";
import {
  subscribeToBoardActivity,
  subscribeToBoardLists,
  subscribeToBoardMeta,
  subscribeToBoards,
  subscribeToList,
} from "../services/websocket";

const ACTIVITY_SIZE = 8;
const SEARCH_SIZE = 10;

const emptyPage = (size) => ({
  content: [],
  page: 0,
  size,
  totalElements: 0,
  totalPages: 0,
  last: true,
});

const sortByPosition = (items) =>
  [...items].sort((a, b) => (a.position ?? 0) - (b.position ?? 0));

const formatTimestamp = (value) => (value ? new Date(value).toLocaleString() : "");

function Dashboard({ logout }) {
  const navigate = useNavigate(); // ✅ ADDED

  // ✅ NEW: Token check on load
  useEffect(() => {
    const token = localStorage.getItem("token");

    if (!token) {
      navigate("/"); // redirect to login
    }
  }, [navigate]);

  const [boards, setBoards] = useState([]);
  const [selectedBoardId, setSelectedBoardId] = useState(null);
  const [boardMeta, setBoardMeta] = useState(null);
  const [lists, setLists] = useState([]);
  const [tasksByList, setTasksByList] = useState({});
  const [activityPage, setActivityPage] = useState(emptyPage(ACTIVITY_SIZE));
  const [searchPage, setSearchPage] = useState(emptyPage(SEARCH_SIZE));
  const [loading, setLoading] = useState(true);
  const [activityLoading, setActivityLoading] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [newBoardName, setNewBoardName] = useState("");
  const [boardNameDraft, setBoardNameDraft] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [newListName, setNewListName] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchListId, setSearchListId] = useState("");
  const [searchAssigneeId, setSearchAssigneeId] = useState("");

  // ❌ NO OTHER CHANGES BELOW (YOUR ORIGINAL CODE CONTINUES)

  const selectedBoard = boards.find((board) => board.id === selectedBoardId) ?? null;
  const boardMembers = boardMeta?.members ?? [];
  const isBoardOwner = selectedBoard?.role === "OWNER";

  const applyBoardsState = useCallback((nextBoards, preferredBoardId = null) => {
    setBoards(nextBoards);
    setSelectedBoardId((currentBoardId) => {
      if (preferredBoardId && nextBoards.some((board) => board.id === preferredBoardId)) {
        return preferredBoardId;
      }

      if (currentBoardId && nextBoards.some((board) => board.id === currentBoardId)) {
        return currentBoardId;
      }

      return nextBoards[0]?.id ?? null;
    });

    if (nextBoards.length === 0) {
      setBoardMeta(null);
      setLists([]);
      setTasksByList({});
      setActivityPage(emptyPage(ACTIVITY_SIZE));
      setSearchPage(emptyPage(SEARCH_SIZE));
      setLoading(false);
    }
  }, []);

  const applyBoardMeta = useCallback((nextMeta) => {
    setBoardMeta(nextMeta);
    setBoardNameDraft(nextMeta?.name ?? "");

    if (!nextMeta) {
      return;
    }

    setBoards((prev) =>
      prev.map((board) =>
        board.id === nextMeta.id
          ? { ...board, name: nextMeta.name, memberCount: nextMeta.members.length }
          : board
      )
    );
  }, []);

  const applyListsState = useCallback((nextLists) => {
    const ordered = sortByPosition(nextLists);
    setLists(ordered);
    setTasksByList((prev) =>
      ordered.reduce((acc, list) => {
        acc[list.id] = sortByPosition(prev[list.id] || []);
        return acc;
      }, {})
    );
  }, []);

  const mergeTask = useCallback((task) => {
    if (!task) {
      return;
    }

    setTasksByList((prev) => {
      const next = Object.fromEntries(
        Object.entries(prev).map(([listId, tasks]) => [
          listId,
          tasks.filter((item) => item.id !== task.id),
        ])
      );

      next[task.listId] = sortByPosition([...(next[task.listId] || []), task]);
      return next;
    });
  }, []);

 const fetchBoards = useCallback(async (preferredBoardId = null, silent = false) => {
  const token = localStorage.getItem("token");

  // ✅ Stop if no token
  if (!token) {
    navigate("/");
    return;
  }

  if (!silent) {
    setLoading(true);
  }

  try {
    setErrorMessage("");
    const res = await API.get("/api/boards");
    applyBoardsState(res.data, preferredBoardId);
  } catch (error) {
    console.error(error);

    // ✅ Ignore 401 (handled by interceptor)
    if (error.response?.status === 401) {
      return;
    }

    setErrorMessage(error.response?.data?.message || "Unable to load boards.");
  } finally {
    if (!silent) {
      setLoading(false);
    }
  }
}, [applyBoardsState, navigate]);

  const fetchActivity = useCallback(async (boardId, page = 0, append = false) => {
    if (!boardId) {
      setActivityPage(emptyPage(ACTIVITY_SIZE));
      return;
    }

    setActivityLoading(true);
    try {
      const res = await API.get(`/api/boards/${boardId}/activity`, {
        params: { page, size: ACTIVITY_SIZE },
      });

      setActivityPage((prev) =>
        append ? { ...res.data, content: [...prev.content, ...res.data.content] } : res.data
      );
    } catch (error) {
      console.error(error);
    } finally {
      setActivityLoading(false);
    }
  }, []);

  const fetchBoardData = useCallback(async (boardId, silent = false) => {
    if (!boardId) {
      setBoardMeta(null);
      setLists([]);
      setTasksByList({});
      setActivityPage(emptyPage(ACTIVITY_SIZE));
      setSearchPage(emptyPage(SEARCH_SIZE));
      setLoading(false);
      return;
    }

    if (!silent) {
      setLoading(true);
    }

    try {
      const [metaRes, listsRes, activityRes] = await Promise.all([
        API.get(`/api/boards/${boardId}/meta`),
        API.get(`/api/lists/${boardId}`),
        API.get(`/api/boards/${boardId}/activity`, {
          params: { page: 0, size: ACTIVITY_SIZE },
        }),
      ]);

      const orderedLists = sortByPosition(listsRes.data);
      applyBoardMeta(metaRes.data);
      applyListsState(orderedLists);
      setActivityPage(activityRes.data);

      const taskResponses = await Promise.all(
        orderedLists.map((list) => API.get(`/api/tasks/${list.id}`))
      );

      setTasksByList(
        orderedLists.reduce((acc, list, index) => {
          acc[list.id] = sortByPosition(taskResponses[index].data || []);
          return acc;
        }, {})
      );

      setSearchPage(emptyPage(SEARCH_SIZE));
    } catch (error) {
      console.error(error);
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, [applyBoardMeta, applyListsState]);

  const runSearch = useCallback(async (page = 0, boardId = selectedBoardId) => {
    if (!boardId) {
      setSearchPage(emptyPage(SEARCH_SIZE));
      return;
    }

    const trimmedQuery = searchQuery.trim();
    if (!trimmedQuery && !searchListId && !searchAssigneeId) {
      setSearchPage(emptyPage(SEARCH_SIZE));
      return;
    }

    setSearchLoading(true);
    try {
      const params = { page, size: SEARCH_SIZE };
      if (trimmedQuery) params.query = trimmedQuery;
      if (searchListId) params.listId = Number(searchListId);
      if (searchAssigneeId) params.assigneeId = Number(searchAssigneeId);

      const res = await API.get(`/api/tasks/board/${boardId}/search`, { params });
      setSearchPage(res.data);
    } catch (error) {
      console.error(error);
    } finally {
      setSearchLoading(false);
    }
  }, [searchAssigneeId, searchListId, searchQuery, selectedBoardId]);

  const createBoard = async () => {
    if (!newBoardName.trim()) return;
    try {
      setErrorMessage("");
      const res = await API.post("/api/boards", { name: newBoardName.trim() });
      const createdBoard = res.data;
      setNewBoardName("");
      applyBoardsState(
        [...boards.filter((board) => board.id !== createdBoard.id), createdBoard].sort((left, right) =>
          left.name.localeCompare(right.name)
        ),
        createdBoard.id
      );
    } catch (error) {
      console.error(error);
      setErrorMessage(error.response?.data?.message || "Unable to create board.");
    }
  };

  const renameBoard = async () => {
    if (!selectedBoardId || !boardNameDraft.trim()) return;
    try {
      await API.put(`/api/boards/${selectedBoardId}`, { name: boardNameDraft.trim() });
    } catch (error) {
      console.error(error);
    }
  };

  const deleteBoard = async () => {
    if (!selectedBoardId) return;
    if (!window.confirm("Delete this board and everything inside it?")) return;

    try {
      await API.delete(`/api/boards/${selectedBoardId}`);
      await fetchBoards(null, true);
    } catch (error) {
      console.error(error);
    }
  };

  const inviteMember = async () => {
    if (!selectedBoardId || !inviteEmail.trim()) return;
    try {
      await API.post(`/api/boards/${selectedBoardId}/members`, {
        email: inviteEmail.trim(),
      });
      setInviteEmail("");
    } catch (error) {
      console.error(error);
    }
  };

  const removeMember = async (userId) => {
    try {
      await API.delete(`/api/boards/${selectedBoardId}/members/${userId}`);
    } catch (error) {
      console.error(error);
    }
  };

  const createList = async () => {
    if (!selectedBoardId || !newListName.trim()) return;
    try {
      await API.post(`/api/lists/${selectedBoardId}`, { name: newListName.trim() });
      setNewListName("");
    } catch (error) {
      console.error(error);
    }
  };

  const deleteList = async (listId) => {
    const tasks = tasksByList[listId] || [];
    if (tasks.length > 0 && !window.confirm("Delete this list and its tasks?")) return;
    try {
      await API.delete(`/api/lists/${listId}`);
    } catch (error) {
      console.error(error);
    }
  };

  const createTask = async (listId, title) => {
    if (!title.trim()) return;
    try {
      await API.post(`/api/tasks/${listId}`, {
        title: title.trim(),
        description: "Created",
      });
    } catch (error) {
      console.error(error);
    }
  };

  const deleteTask = async (taskId) => {
    try {
      await API.delete(`/api/tasks/${taskId}`);
    } catch (error) {
      console.error(error);
    }
  };

  const moveTask = async (taskId, targetListId, targetPosition) => {
    const res = await API.put(`/api/tasks/${taskId}/move`, {
      targetListId,
      targetPosition,
    });
    mergeTask(res.data);
  };

  const assignTask = async (taskId, userId) => {
    try {
      const res = await API.post(`/api/tasks/${taskId}/assignees`, { userId });
      mergeTask(res.data);
    } catch (error) {
      console.error(error);
    }
  };

  const unassignTask = async (taskId, userId) => {
    try {
      const res = await API.delete(`/api/tasks/${taskId}/assignees/${userId}`);
      mergeTask(res.data);
    } catch (error) {
      console.error(error);
    }
  };

  const reorderLists = async (orderedLists) => {
    try {
      await API.put(`/api/lists/reorder/${selectedBoardId}`, {
        listIds: orderedLists.map((list) => list.id),
      });
    } catch (error) {
      console.error(error);
      await fetchBoardData(selectedBoardId, true);
    }
  };

  const handleDropdownMove = async (taskId, sourceListId, targetListId) => {
    if (sourceListId === targetListId) return;
    try {
      await moveTask(taskId, targetListId);
    } catch (error) {
      console.error(error);
      await fetchBoardData(selectedBoardId, true);
    }
  };

  const onDragEnd = async (result) => {
    if (!result.destination) return;

    const { source, destination, type, draggableId } = result;

    if (type === "LIST") {
      if (source.index === destination.index) return;
      const nextLists = Array.from(lists);
      const [movedList] = nextLists.splice(source.index, 1);
      nextLists.splice(destination.index, 0, movedList);
      setLists(nextLists);
      await reorderLists(nextLists);
      return;
    }

    const sourceListId = Number(source.droppableId);
    const destinationListId = Number(destination.droppableId);
    const taskId = Number(draggableId);

    if (
      Number.isNaN(taskId) ||
      (sourceListId === destinationListId && source.index === destination.index)
    ) {
      return;
    }

    setTasksByList((prev) => {
      const next = { ...prev };
      const sourceTasks = Array.from(next[sourceListId] || []);
      const destinationTasks =
        sourceListId === destinationListId
          ? sourceTasks
          : Array.from(next[destinationListId] || []);

      const [movedTask] = sourceTasks.splice(source.index, 1);
      if (!movedTask) return prev;

      destinationTasks.splice(destination.index, 0, movedTask);
      next[sourceListId] = sourceTasks;
      next[destinationListId] = destinationTasks;
      return next;
    });

    try {
      await moveTask(taskId, destinationListId, destination.index + 1);
    } catch (error) {
      console.error(error);
      await fetchBoardData(selectedBoardId, true);
    }
  };

  useEffect(() => {
    fetchBoards();
  }, [fetchBoards]);

  useEffect(() => {
    fetchBoardData(selectedBoardId);
  }, [fetchBoardData, selectedBoardId]);

  useEffect(
    () =>
      subscribeToBoards((event) => {
        if (event.type === "SYNC") applyBoardsState(event.boards || []);
      }),
    [applyBoardsState]
  );

  useEffect(() => {
    if (!selectedBoardId) return undefined;
    return subscribeToBoardMeta(selectedBoardId, (event) => {
      if (event.type === "SYNC") applyBoardMeta(event.board || null);
    });
  }, [applyBoardMeta, selectedBoardId]);

  useEffect(() => {
    if (!selectedBoardId) return undefined;
    return subscribeToBoardLists(selectedBoardId, (event) => {
      if (event.type === "SYNC") applyListsState(event.lists || []);
    });
  }, [applyListsState, selectedBoardId]);

  useEffect(() => {
    if (!selectedBoardId) return undefined;
    return subscribeToBoardActivity(selectedBoardId, (event) => {
      if (event.type !== "CREATED" || !event.activity) return;
      setActivityPage((prev) => {
        const exists = prev.content.some((item) => item.id === event.activity.id);
        return {
          ...prev,
          content: [event.activity, ...prev.content.filter((item) => item.id !== event.activity.id)].slice(0, prev.size),
          totalElements: exists ? prev.totalElements : prev.totalElements + 1,
        };
      });
    });
  }, [selectedBoardId]);

  useEffect(() => {
    if (lists.length === 0) return undefined;

    const unsubscribes = lists.map((list) =>
      subscribeToList(list.id, (event) => {
        if (event.type !== "SYNC") return;
        setTasksByList((prev) => ({
          ...prev,
          [list.id]: sortByPosition(event.tasks || []),
        }));
      })
    );

    return () => unsubscribes.forEach((unsubscribe) => unsubscribe?.());
  }, [lists]);

  if (loading) {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="text-lg font-semibold">Loading dashboard...</p>
    </div>
  );
}

  if (!loading && boards.length === 0) {
    return (
      <div className="min-h-screen bg-slate-100 p-8">
        <div className="mx-auto max-w-5xl rounded-2xl bg-white p-6 shadow">
          <div className="mb-4 flex items-center justify-between">
            <h1 className="text-3xl font-bold">Task Dashboard</h1>
            <button onClick={logout} className="rounded bg-slate-900 px-4 py-2 text-white">
              Logout
            </button>
          </div>
          {!loading && errorMessage && (
            <p className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {errorMessage}
            </p>
          )}
          <div className="flex gap-2">
            <input
              value={newBoardName}
              onChange={(e) => setNewBoardName(e.target.value)}
              placeholder="Create a new board"
              className="flex-1 rounded border p-2"
            />
            <button onClick={createBoard} className="rounded bg-blue-600 px-4 py-2 text-white">
              Create
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-r from-blue-100 to-purple-100">
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="rounded-2xl bg-white p-4 shadow">
          <div className="mb-4 flex items-center justify-between gap-4">
            <h1 className="text-3xl font-bold">Task Dashboard</h1>
            <button onClick={logout} className="rounded bg-purple-600 px-4 py-2 text-white hover:bg-purple-700">
              Logout
            </button>
          </div>
          {errorMessage && (
            <p className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {errorMessage}
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            <input
              value={newBoardName}
              onChange={(e) => setNewBoardName(e.target.value)}
              placeholder="New board"
              className="flex-1 rounded border p-2"
            />
            <button onClick={createBoard} className="rounded bg-blue-500 px-4 py-2 text-white hover:bg-blue-600">
              Create Board
            </button>
          </div>
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
          <div className="space-y-6">
            <div className="rounded-2xl bg-white p-4 shadow">
              <div className="flex flex-wrap gap-2">
                <select
                  value={selectedBoardId || ""}
                  onChange={(e) => setSelectedBoardId(Number(e.target.value))}
                  className="min-w-72 rounded border p-2"
                >
                  {boards.map((board) => (
                    <option key={board.id} value={board.id}>
                      {board.name} - {board.role} - {board.memberCount} members
                    </option>
                  ))}
                </select>

                <input
                  value={boardNameDraft}
                  onChange={(e) => setBoardNameDraft(e.target.value)}
                  disabled={!isBoardOwner}
                  placeholder="Rename board"
                  className="flex-1 rounded border p-2 disabled:bg-slate-100"
                />

                {isBoardOwner && (
                  <button onClick={renameBoard} className="rounded bg-blue-400 px-4 py-2 text-white hover:bg-blue-500">
                    Save
                  </button>
                )}

                {isBoardOwner && (
                  <button onClick={deleteBoard} className="rounded bg-red-500 px-4 py-2 text-white hover:bg-red-600">
                    Delete
                  </button>
                )}
              </div>
            </div>

            <div className="rounded-2xl bg-white p-4 shadow">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h2 className="text-xl font-semibold">{selectedBoard?.name}</h2>
                  <p className="text-sm text-slate-500">{selectedBoard?.role}</p>
                </div>

                <div className="flex gap-2">
                  <input
                    value={newListName}
                    onChange={(e) => setNewListName(e.target.value)}
                    placeholder="New list"
                    className="rounded border p-2"
                  />
                  <button onClick={createList} className="rounded bg-emerald-500 px-4 py-2 text-white hover:bg-emerald-600">
                    Create List
                  </button>
                </div>
              </div>

              <DragDropContext onDragEnd={onDragEnd}>
                <Droppable droppableId="all-lists" direction="horizontal" type="LIST">
                  {(provided) => (
                    <div ref={provided.innerRef} {...provided.droppableProps} className="flex gap-6 overflow-x-auto pb-4">
                      {lists.map((list, listIndex) => (
                        <Draggable key={list.id} draggableId={`list-${list.id}`} index={listIndex}>
                          {(listProvided) => (
                            <div
                              ref={listProvided.innerRef}
                              {...listProvided.draggableProps}
                              className="w-64 flex-shrink-0 rounded-2xl bg-gradient-to-br from-slate-100 to-blue-100 p-4 shadow-md"
                            >
                              <div {...listProvided.dragHandleProps} className="mb-3 flex items-center justify-between">
                                <div>
                                  <h3 className="text-lg font-semibold">{list.name}</h3>
                                  <p className="text-sm text-slate-500">{(tasksByList[list.id] || []).length} tasks</p>
                                </div>
                                <button
  onClick={() => deleteList(list.id)}
  className="bg-red-500 hover:bg-red-600 text-white text-xs px-3 py-1 rounded-md transition duration-200"
>
  Delete
</button>
                              </div>

                              <Droppable droppableId={String(list.id)} type="TASK">
                                {(taskDropProvided) => (
                                  <div ref={taskDropProvided.innerRef} {...taskDropProvided.droppableProps} className="min-h-24 space-y-3 rounded-2xl bg-white p-3">
                                    {(tasksByList[list.id] || []).map((task, taskIndex) => {
                                      const assigned = new Set((task.assignees || []).map((item) => item.id));

                                      return (
                                        <Draggable key={task.id} draggableId={String(task.id)} index={taskIndex}>
                                          {(taskProvided) => (
                                            <div
                                              ref={taskProvided.innerRef}
                                              {...taskProvided.draggableProps}
                                              {...taskProvided.dragHandleProps}
                                              className="rounded-xl border bg-slate-50 p-3"
                                            >
                                              <div className="flex items-start justify-between gap-2">
                                                <div>
                                                  <p className="font-semibold">{task.title}</p>
                                                  <p className="text-sm text-slate-600">{task.description}</p>
                                                </div>
                                                <button
  onClick={() => deleteTask(task.id)}
  className="bg-red-500 hover:bg-red-600 text-white text-xs px-2 py-1 rounded-md transition duration-200"
>
  Delete
</button>
                                              </div>

                                              <div className="mt-2 flex flex-wrap gap-2">
                                                {(task.assignees || []).map((member) => (
                                                  <button
                                                    key={member.id}
                                                    onClick={() => unassignTask(task.id, member.id)}
                                                    className="rounded-full bg-sky-100 px-2 py-1 text-xs"
                                                  >
                                                    {member.name} x
                                                  </button>
                                                ))}
                                                {task.assignees?.length === 0 && (
                                                  <span className="rounded-full bg-slate-200 px-2 py-1 text-xs">
                                                    Unassigned
                                                  </span>
                                                )}
                                              </div>

                                              <div className="mt-2 flex flex-wrap gap-2">
                                                <select
                                                  defaultValue=""
                                                  onChange={(e) => {
                                                    const userId = Number(e.target.value);
                                                    if (userId) {
                                                      assignTask(task.id, userId);
                                                      e.target.value = "";
                                                    }
                                                  }}
                                                  className="rounded border px-2 py-1 text-sm"
                                                >
                                                  <option value="">Assign member</option>
                                                  {boardMembers
                                                    .filter((member) => !assigned.has(member.userId))
                                                    .map((member) => (
                                                      <option key={member.userId} value={member.userId}>
                                                        {member.name}
                                                      </option>
                                                    ))}
                                                </select>

                                                <select
                                                  value={list.id}
                                                  onChange={(e) => handleDropdownMove(task.id, list.id, Number(e.target.value))}
                                                  className="rounded border px-2 py-1 text-sm"
                                                >
                                                  {lists.map((targetList) => (
                                                    <option key={targetList.id} value={targetList.id}>
                                                      {targetList.name}
                                                    </option>
                                                  ))}
                                                </select>
                                              </div>
                                            </div>
                                          )}
                                        </Draggable>
                                      );
                                    })}

                                    {taskDropProvided.placeholder}
                                  </div>
                                )}
                              </Droppable>

                              <input
                                placeholder="New task"
                                className="mt-3 w-full rounded border p-2"
                                onKeyDown={(e) => {
                                  if (e.key === "Enter") {
                                    createTask(list.id, e.target.value);
                                    e.target.value = "";
                                  }
                                }}
                              />
                            </div>
                          )}
                        </Draggable>
                      ))}
                      {provided.placeholder}
                    </div>
                  )}
                </Droppable>
              </DragDropContext>
            </div>
          </div>

          <div className="space-y-6">
            <div className="rounded-2xl bg-white p-4 shadow">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Members</h2>
                <span className="text-sm text-slate-500">{boardMembers.length}</span>
              </div>

              {isBoardOwner && (
                <div className="mb-3 flex gap-2">
                  <input
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    placeholder="Invite by email"
                    className="flex-1 rounded border p-2"
                  />
                  <button onClick={inviteMember} className="rounded bg-blue-500 px-4 py-2 text-white hover:bg-blue-600">
                    Add
                  </button>
                </div>
              )}

              <div className="space-y-2">
                {boardMembers.map((member) => (
                  <div key={member.userId} className="flex items-center justify-between rounded bg-slate-50 p-3">
                    <div>
                      <p className="font-medium">{member.name}</p>
                      <p className="text-sm text-slate-500">{member.email}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="rounded-full bg-slate-200 px-2 py-1 text-xs">{member.role}</span>
                      {isBoardOwner && member.role !== "OWNER" && (
                        <button onClick={() => removeMember(member.userId)} className="text-sm text-red-600">
                          Remove
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-2xl bg-white p-4 shadow">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Search Tasks</h2>
                {searchLoading && <span className="text-sm text-slate-500">Searching...</span>}
              </div>

              <div className="space-y-2">
                <input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} placeholder="Title or description" className="w-full rounded border p-2" />
                <select value={searchListId} onChange={(e) => setSearchListId(e.target.value)} className="w-full rounded border p-2">
                  <option value="">All lists</option>
                  {lists.map((list) => (
                    <option key={list.id} value={list.id}>{list.name}</option>
                  ))}
                </select>
                <select value={searchAssigneeId} onChange={(e) => setSearchAssigneeId(e.target.value)} className="w-full rounded border p-2">
                  <option value="">All assignees</option>
                  {boardMembers.map((member) => (
                    <option key={member.userId} value={member.userId}>{member.name}</option>
                  ))}
                </select>
                <div className="flex gap-2">
                  <button onClick={() => runSearch(0)} className="rounded bg-slate-900 px-4 py-2 text-white">
                    Search
                  </button>
                  <button
                    onClick={() => {
                      setSearchQuery("");
                      setSearchListId("");
                      setSearchAssigneeId("");
                      setSearchPage(emptyPage(SEARCH_SIZE));
                    }}
                    className="rounded border px-4 py-2"
                  >
                    Clear
                  </button>
                </div>
              </div>

              <div className="mt-3 space-y-2">
                {searchPage.content.map((task) => (
                  <div key={`search-${task.id}`} className="rounded bg-slate-50 p-3">
                    <p className="font-medium">{task.title}</p>
                    <p className="text-sm text-slate-600">{task.listName}</p>
                  </div>
                ))}
                {searchPage.content.length === 0 && (
                  <p className="text-sm text-slate-500">No results yet.</p>
                )}
              </div>

              {searchPage.content.length > 0 && (
                <div className="mt-3 flex items-center justify-between text-sm">
                  <button disabled={searchPage.page === 0} onClick={() => runSearch(searchPage.page - 1)} className="rounded border px-3 py-1 disabled:opacity-40">
                    Previous
                  </button>
                  <span>Page {searchPage.page + 1} / {Math.max(searchPage.totalPages, 1)}</span>
                  <button disabled={searchPage.last} onClick={() => runSearch(searchPage.page + 1)} className="rounded border px-3 py-1 disabled:opacity-40">
                    Next
                  </button>
                </div>
              )}
            </div>

            <div className="rounded-2xl bg-white p-4 shadow">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Activity</h2>
                {activityLoading && <span className="text-sm text-slate-500">Loading...</span>}
              </div>

              <div className="space-y-2">
                {activityPage.content.map((activity) => (
                  <div key={activity.id} className="rounded bg-slate-50 p-3">
                    <p className="text-sm">{activity.description}</p>
                    <p className="mt-1 text-xs text-slate-500">
                      {activity.user?.name} - {formatTimestamp(activity.createdAt)}
                    </p>
                  </div>
                ))}
                {activityPage.content.length === 0 && (
                  <p className="text-sm text-slate-500">No activity yet.</p>
                )}
              </div>

              {!activityPage.last && (
                <button
                  onClick={() => fetchActivity(selectedBoardId, activityPage.page + 1, true)}
                  className="mt-3 rounded border px-4 py-2"
                >
                  Load More
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;

