let session = { accessToken: "", user: null };
export const getSession = () => session;
export function clearSession() {
  session = { accessToken: "", user: null };
}

// FE-02: 토큰이 있을 때만 Bearer 헤더를 만든다 (없으면 헤더 자체를 안 보냄)
function authorizationHeaders() {
  return session.accessToken
      ? { Authorization: `Bearer ${session.accessToken}` }
      : {};
}

export async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authorizationHeaders(),
      ...options.headers,
    },
  });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    // FE-03: 401이면 메모리의 토큰과 사용자를 지운다 (403은 로그인 유지)
    if (response.status === 401) clearSession();
    const error = new Error(
        data.message || data.code || `HTTP ${response.status}`,
    );
    error.status = response.status;
    error.code = data.code;
    throw error;
  }
  return data;
}

// FE-01: 로그인 응답의 토큰과 사용자를 메모리(session)에만 저장한다
export async function login(email, password) {
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  session = { accessToken: data.accessToken, user: data.user };
  return data.user;
}

// FE-04: 로그인한 사용자 id와 글의 작성자 id를 비교한다 (서버에서도 다시 검사함)
export function canEdit(post) {
  const user = session.user;
  return !!user && !!post && user.id === post.authorId;
}

export const api = {
  list: () => request("/posts"),
  detail: (id) => request(`/posts/${id}`),
  create: (input) =>
      request("/posts", { method: "POST", body: JSON.stringify(input) }),
  update: (id, input) =>
      request(`/posts/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  remove: (id) => request(`/posts/${id}`, { method: "DELETE" }),
  me: () => request("/me"),
};